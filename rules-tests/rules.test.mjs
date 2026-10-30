// Firestore rules tests. Run with:  npm test   (starts the emulator; needs Java: JAVA_HOME or PATH)
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, query, where,
  serverTimestamp, writeBatch, Timestamp, deleteField,
} from 'firebase/firestore';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { test, before, after, beforeEach, describe } from 'node:test';
import assert from 'node:assert/strict';

const U = 'user1', O = 'obs1', X = 'stranger';
const emailOf = (uid) => `${uid}@example.com`;
const sha = (s) => createHash('sha256').update(s.toLowerCase()).digest('hex');
const HOUR = 3600 * 1000;
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-focuslock',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080 },
  });
});
after(async () => { await env.cleanup(); });

const db = (uid) => env.authenticatedContext(uid, { email: emailOf(uid), email_verified: true }).firestore();
const anon = () => env.unauthenticatedContext().firestore();
const seed = (fn) => env.withSecurityRulesDisabled((c) => fn(c.firestore()));

const profile = (code) => ({ displayName: 'Name', email: 'a@b.co', friendCode: code, createdAt: serverTimestamp(), deviceId: 'dev1' });
const session = (by, over = {}) => ({
  createdBy: by, mode: 'full', kind: 'instant', startAt: Date.now(), endAt: Date.now() + HOUR,
  status: 'active', remoteInstant: false, ...over,
});

async function seedBase(linkStatus = 'active', extra = {}) {
  await seed(async (d) => {
    for (const [uid, code] of [[U, 'AAAAAAAA'], [O, 'BBBBBBBB'], [X, 'CCCCCCCC']]) {
      await setDoc(doc(d, 'users', uid), { displayName: uid, email: emailOf(uid), friendCode: code, createdAt: Timestamp.now(), deviceId: 'd-' + uid });
    }
    if (linkStatus) await setDoc(doc(d, 'links', U), { observerUid: O, status: linkStatus, createdAt: Timestamp.now(), ...extra });
  });
}

beforeEach(async () => { await env.clearFirestore(); });

describe('users', () => {
  test('self creates valid profile; others/invalid denied', async () => {
    await assertSucceeds(setDoc(doc(db(U), 'users', U), profile('ABCDEFGH')));
    await assertFails(setDoc(doc(db(X), 'users', U), profile('ABCDEFGH')));
    await assertFails(setDoc(doc(db(U), 'users', U), profile('abc')));
    await assertFails(setDoc(doc(db(U), 'users', U), profile('ABCDEFG0'))); // 0 not in alphabet
    await assertFails(setDoc(doc(db(U), 'users', U), { ...profile('ABCDEFGH'), admin: true }));
    await assertFails(setDoc(doc(anon(), 'users', U), profile('ABCDEFGH')));
  });

  test('read access: self, active observer, user->own observer; not strangers/pending observer', async () => {
    await seedBase('active');
    await assertSucceeds(getDoc(doc(db(U), 'users', U)));
    await assertSucceeds(getDoc(doc(db(O), 'users', U)));
    await assertSucceeds(getDoc(doc(db(U), 'users', O)));
    await assertFails(getDoc(doc(db(X), 'users', U)));
    await assertFails(getDoc(doc(db(U), 'users', X)));
    await assertFails(getDocs(collection(db(U), 'users')));
    await env.clearFirestore();
    await seedBase('pending');
    await assertFails(getDoc(doc(db(O), 'users', U)));
    await assertSucceeds(getDoc(doc(db(U), 'users', O))); // User must see who is asking
  });

  test('only displayName/email are mutable; no delete', async () => {
    await seedBase('active');
    await assertSucceeds(updateDoc(doc(db(U), 'users', U), { displayName: 'New' }));
    await assertFails(updateDoc(doc(db(U), 'users', U), { friendCode: 'ZZZZZZZZ' }));
    await assertFails(updateDoc(doc(db(U), 'users', U), { deviceId: 'other' }));
    await assertFails(updateDoc(doc(db(O), 'users', U), { displayName: 'hax' }));
    await assertFails(deleteDoc(doc(db(U), 'users', U)));
  });
});

describe('lookup', () => {
  test('email key must equal sha256 of own verified email', async () => {
    await assertSucceeds(setDoc(doc(db(U), 'lookup', sha(emailOf(U))), { uid: U }));
    await assertFails(setDoc(doc(db(U), 'lookup', sha(emailOf(O))), { uid: U }));
    await assertFails(setDoc(doc(db(U), 'lookup', sha(emailOf(U)).replace(/.$/, '0')), { uid: U }));
    await assertFails(setDoc(doc(db(O), 'lookup', sha(emailOf(U))), { uid: O }));
    await assertFails(setDoc(doc(db(U), 'lookup', sha(emailOf(U))), { uid: U, extra: 1 }));
  });

  test('unverified email cannot claim an email key', async () => {
    const d = env.authenticatedContext(U, { email: emailOf(U), email_verified: false }).firestore();
    await assertFails(setDoc(doc(d, 'lookup', sha(emailOf(U))), { uid: U }));
  });

  test('friend code key must match own profile (same batch)', async () => {
    const d = db(U);
    const b = writeBatch(d);
    b.set(doc(d, 'users', U), profile('ABCDEFGH'));
    b.set(doc(d, 'lookup', 'ABCDEFGH'), { uid: U });
    await assertSucceeds(b.commit());
    const b2 = writeBatch(d);
    b2.set(doc(d, 'lookup', 'ZZZZZZZZ'), { uid: U });
    await assertFails(b2.commit());
  });

  test('cannot steal an existing code; immutable; get-only', async () => {
    await seedBase(null);
    await seed((d) => setDoc(doc(d, 'lookup', 'AAAAAAAA'), { uid: U }));
    await assertFails(setDoc(doc(db(X), 'lookup', 'AAAAAAAA'), { uid: X }));
    await assertFails(updateDoc(doc(db(U), 'lookup', 'AAAAAAAA'), { uid: X }));
    await assertFails(deleteDoc(doc(db(U), 'lookup', 'AAAAAAAA')));
    await assertSucceeds(getDoc(doc(db(X), 'lookup', 'AAAAAAAA')));
    await assertFails(getDoc(doc(anon(), 'lookup', 'AAAAAAAA')));
    await assertFails(getDocs(collection(db(X), 'lookup')));
  });
});

describe('links', () => {
  test('observer requests pending link; guards', async () => {
    await seedBase(null);
    const req = () => ({ observerUid: O, status: 'pending', createdAt: serverTimestamp() });
    await assertSucceeds(setDoc(doc(db(O), 'links', U), req()));
    await env.clearFirestore(); await seedBase(null);
    await assertFails(setDoc(doc(db(U), 'links', U), { ...req(), observerUid: U }));      // self-link
    await assertFails(setDoc(doc(db(O), 'links', U), { ...req(), status: 'active' }));     // not pending
    await assertFails(setDoc(doc(db(O), 'links', U), { ...req(), observerUid: X }));       // spoofed observer
    await assertFails(setDoc(doc(db(O), 'links', 'ghost'), req()));                        // target has no profile
    await assertFails(setDoc(doc(db(O), 'links', U), { ...req(), createdAt: Timestamp.fromMillis(1) }));
  });

  test('second observer cannot take over an existing link', async () => {
    await seedBase('active');
    await assertFails(setDoc(doc(db(X), 'links', U), { observerUid: X, status: 'pending', createdAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db(O), 'links', U), { observerUid: O, status: 'pending', createdAt: serverTimestamp() }));
  });

  test('only the User accepts a pending link', async () => {
    await seedBase('pending');
    await assertFails(updateDoc(doc(db(O), 'links', U), { status: 'active' }));
    await assertFails(updateDoc(doc(db(X), 'links', U), { status: 'active' }));
    await assertFails(updateDoc(doc(db(U), 'links', U), { status: 'active', observerUid: U }));
    await assertSucceeds(updateDoc(doc(db(U), 'links', U), { status: 'active' }));
  });

  test('read: user and observer only; observer can list own users', async () => {
    await seedBase('active');
    await assertSucceeds(getDoc(doc(db(U), 'links', U)));
    await assertSucceeds(getDoc(doc(db(O), 'links', U)));
    await assertFails(getDoc(doc(db(X), 'links', U)));
    await assertSucceeds(getDocs(query(collection(db(O), 'links'), where('observerUid', '==', O))));
    await assertFails(getDocs(collection(db(O), 'links')));
    await assertFails(getDocs(query(collection(db(X), 'links'), where('observerUid', '==', O))));
  });

  test('decline / cancel pending', async () => {
    await seedBase('pending');
    await assertSucceeds(deleteDoc(doc(db(U), 'links', U)));
    await env.clearFirestore(); await seedBase('pending');
    await assertSucceeds(deleteDoc(doc(db(O), 'links', U)));
    await env.clearFirestore(); await seedBase('pending');
    await assertFails(deleteDoc(doc(db(X), 'links', U)));
  });

  test('removal flow (LINK-6)', async () => {
    await seedBase('active');
    await assertFails(deleteDoc(doc(db(U), 'links', U)));                                 // cannot just leave
    await assertFails(deleteDoc(doc(db(O), 'links', U)));                                 // observer cannot drop an active link
    await assertFails(updateDoc(doc(db(U), 'links', U), { status: 'removal_requested', removalRequestedAt: Timestamp.fromMillis(1) }));
    await assertFails(updateDoc(doc(db(O), 'links', U), { status: 'removal_requested', removalRequestedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(doc(db(U), 'links', U), { status: 'removal_requested', removalRequestedAt: serverTimestamp() }));
    await assertFails(deleteDoc(doc(db(U), 'links', U)));                                 // < 7 days
    await assertSucceeds(deleteDoc(doc(db(O), 'links', U)));                              // observer accepts
  });

  test('observer can decline removal; user can finish after 7 days', async () => {
    await seedBase('removal_requested', { removalRequestedAt: Timestamp.now() });
    await assertFails(updateDoc(doc(db(U), 'links', U), { status: 'active', removalRequestedAt: deleteField() }));
    await assertSucceeds(updateDoc(doc(db(O), 'links', U), { status: 'active', removalRequestedAt: deleteField() }));
    await env.clearFirestore();
    await seedBase('removal_requested', { removalRequestedAt: Timestamp.fromMillis(Date.now() - 8 * 24 * HOUR) });
    await assertSucceeds(deleteDoc(doc(db(U), 'links', U)));
  });
});

describe('sessions', () => {
  const S = (uid = U) => doc(db(uid), 'users', U, 'sessions', 's1');
  const seedSession = (by, over = {}) => seed((d) => setDoc(doc(d, 'users', U, 'sessions', 's1'), session(by, { pwHash: 'h', pwSalt: 's', ...over })));

  test('creation rules', async () => {
    await seedBase('active');
    await assertSucceeds(setDoc(S(O), session(O, { pwHash: 'h', pwSalt: 's' })));
    await assertSucceeds(setDoc(doc(db(O), 'users', U, 'sessions', 's2'), session(O, { mode: 'apps', packages: ['a.b'] })));
    await assertFails(setDoc(doc(db(O), 'users', U, 'sessions', 's3'), session(O, { mode: 'apps' })));             // apps need packages
    await assertFails(setDoc(doc(db(O), 'users', U, 'sessions', 's4'), session(O, { endAt: Date.now() + 25 * HOUR }))); // > 24 h
    await assertFails(setDoc(doc(db(O), 'users', U, 'sessions', 's5'), session(O, { startAt: Date.now() - 2 * HOUR, endAt: Date.now() - HOUR }))); // expired
    await assertFails(setDoc(doc(db(O), 'users', U, 'sessions', 's6'), session(U)));                               // spoofed creator
    await assertFails(setDoc(doc(db(O), 'users', U, 'sessions', 's7'), session(O, { status: 'ended' })));
    await assertFails(setDoc(doc(db(X), 'users', U, 'sessions', 's8'), session(X)));                               // stranger
  });

  test('self-lock: no password, may be ended by user and by observer', async () => {
    await seedBase('active');
    await assertSucceeds(setDoc(doc(db(U), 'users', U, 'sessions', 'me'), session(U)));
    await assertFails(setDoc(doc(db(U), 'users', U, 'sessions', 'me2'), session(U, { pwHash: 'h' })));
    await assertFails(setDoc(doc(db(U), 'users', U, 'sessions', 'me3'), session(U, { remoteInstant: true })));
    await assertFails(updateDoc(doc(db(U), 'users', U, 'sessions', 'me'), { endAt: Date.now() + 100 * HOUR }));   // no extending/editing
    await assertSucceeds(updateDoc(doc(db(U), 'users', U, 'sessions', 'me'), { status: 'ended', endedBy: U }));
    await assertSucceeds(setDoc(doc(db(U), 'users', U, 'sessions', 'me4'), session(U)));
    await assertSucceeds(updateDoc(doc(db(O), 'users', U, 'sessions', 'me4'), { status: 'ended', endedBy: O }));
  });

  test('user cannot end, edit or delete an observer-created session', async () => {
    await seedBase('active'); await seedSession(O);
    await assertFails(updateDoc(S(U), { status: 'ended', endedBy: U }));
    await assertFails(updateDoc(S(U), { status: 'ended', endedBy: O }));       // forged endedBy
    await assertFails(updateDoc(S(U), { endAt: Date.now() - 1000 }));
    await assertFails(updateDoc(S(U), { pwHash: 'mine', pwSalt: 'mine' }));
    await assertFails(deleteDoc(S(U)));
  });

  test('user may only set passwordUsed=true', async () => {
    await seedBase('active'); await seedSession(O);
    await assertSucceeds(updateDoc(S(U), { passwordUsed: true }));
    await assertFails(updateDoc(S(U), { passwordUsed: false }));
    await assertFails(updateDoc(S(U), { passwordUsed: true, status: 'ended', endedBy: O }));
    await assertFails(updateDoc(S(X), { passwordUsed: true }));
  });

  test('observer ends with own uid; spoofing and strangers denied', async () => {
    await seedBase('active'); await seedSession(O);
    await assertFails(updateDoc(S(O), { status: 'ended', endedBy: U }));       // cannot impersonate user
    await assertFails(updateDoc(S(O), { status: 'ended' }));                    // endedBy required
    await assertFails(updateDoc(S(X), { status: 'ended', endedBy: X }));
    await assertSucceeds(updateDoc(S(O), { status: 'ended', endedBy: O }));
    await assertFails(updateDoc(S(O), { status: 'ended', endedBy: O }));        // already ended
  });

  test('observer regenerates password; user cannot', async () => {
    await seedBase('active'); await seedSession(O);
    await assertFails(updateDoc(S(U), { pwHash: 'n', pwSalt: 'n' }));
    await assertSucceeds(updateDoc(S(O), { pwHash: 'n', pwSalt: 'n' }));
  });

  test('observer loses access once link is gone or still pending', async () => {
    await seedBase('pending'); await seedSession(O);
    await assertFails(getDoc(S(O)));
    await assertFails(setDoc(doc(db(O), 'users', U, 'sessions', 'z'), session(O)));
    await env.clearFirestore(); await seedBase('removal_requested', { removalRequestedAt: Timestamp.now() }); await seedSession(O);
    await assertSucceeds(getDoc(S(O)));                                          // still enforced during removal (LINK-7)
    await assertSucceeds(getDoc(S(U)));
    await assertFails(getDoc(S(X)));
  });
});

describe('schedules, formats, apps', () => {
  const sched = (by, over = {}) => ({ createdBy: by, mode: 'full', startMin: 600, endMin: 660, tz: 'Asia/Dhaka', enabled: true, days: [1, 2], ...over });
  const col = (uid, c, id) => doc(db(uid), 'users', U, c, id);

  test('schedule ownership (SCH-2) and password pool', async () => {
    await seedBase('active');
    await assertSucceeds(setDoc(col(O, 'schedules', 'a'), sched(O, { pwPool: { '2026-11-01': { h: 'x', s: 'y' } } })));
    await assertSucceeds(setDoc(col(U, 'schedules', 'b'), sched(U)));
    await assertFails(setDoc(col(U, 'schedules', 'c'), sched(U, { pwPool: {} })));       // user cannot hold a pool
    await assertFails(setDoc(col(U, 'schedules', 'd'), sched(O)));                       // spoofed creator
    await assertFails(setDoc(col(U, 'schedules', 'e'), sched(U, { startMin: 2000 })));
    await assertFails(updateDoc(col(U, 'schedules', 'a'), { enabled: false }));          // user cannot edit observer's
    await assertFails(deleteDoc(col(U, 'schedules', 'a')));
    await assertFails(updateDoc(col(O, 'schedules', 'b'), { enabled: false }));          // observer cannot edit user's
    await assertFails(deleteDoc(col(O, 'schedules', 'b')));
    await assertSucceeds(updateDoc(col(O, 'schedules', 'a'), { enabled: false }));
    await assertSucceeds(deleteDoc(col(U, 'schedules', 'b')));
    await assertFails(getDoc(col(X, 'schedules', 'a')));
  });

  test('formats editable by both (FMT-2), not by strangers', async () => {
    await seedBase('active');
    await assertSucceeds(setDoc(col(U, 'formats', 'f'), { name: 'Study', packages: ['a.b'], createdBy: U }));
    await assertSucceeds(updateDoc(col(O, 'formats', 'f'), { packages: ['a.b', 'c.d'] }));
    await assertFails(updateDoc(col(O, 'formats', 'f'), { createdBy: O }));
    await assertFails(setDoc(col(X, 'formats', 'g'), { name: 'x', packages: [], createdBy: X }));
    await assertSucceeds(deleteDoc(col(O, 'formats', 'f')));
  });

  test('app list: only the phone writes; observer reads', async () => {
    await seedBase('active');
    await assertSucceeds(setDoc(col(U, 'apps', 'list'), { items: [{ n: 'A', p: 'a.b' }], updatedAt: serverTimestamp() }));
    await assertFails(setDoc(col(U, 'apps', 'other'), { items: [], updatedAt: serverTimestamp() }));
    await assertFails(setDoc(col(O, 'apps', 'list'), { items: [], updatedAt: serverTimestamp() }));
    await assertSucceeds(getDoc(col(O, 'apps', 'list')));
    await assertFails(getDoc(col(X, 'apps', 'list')));
  });
});

describe('app query shapes (T09/T10b)', () => {
  const sess = (d, id, over) => setDoc(doc(d, 'users', U, 'sessions', id), session(O, { remoteInstant: true, ...over }));

  test('remote instant: observer creates pending session starting in 60 s; user and observer see it via endAt query', async () => {
    await seedBase('active');
    const start = Date.now() + 60000;
    await assertSucceeds(sess(db(O), 'r1', { status: 'pending', startAt: start, endAt: start + HOUR }));
    const q = (uid) => query(collection(db(uid), 'users', U, 'sessions'), where('endAt', '>', Date.now() - 60000));
    for (const uid of [U, O]) assert.equal((await assertSucceeds(getDocs(q(uid)))).size, 1);
    await assertFails(getDocs(query(collection(db(X), 'users', U, 'sessions'), where('endAt', '>', 0))));
  });

  test('observer unlock; user cannot unlock a remote session', async () => {
    await seedBase('active');
    await seed((d) => sess(d, 'r2', { status: 'pending' }));
    await assertFails(updateDoc(doc(db(U), 'users', U, 'sessions', 'r2'), { status: 'ended', endedBy: U }));
    await assertSucceeds(updateDoc(doc(db(O), 'users', U, 'sessions', 'r2'), { status: 'ended', endedBy: O }));
  });

  test('links: observer lists their users by observerUid; others cannot list; own link readable when missing', async () => {
    await seedBase('pending');
    assert.equal((await assertSucceeds(getDocs(query(collection(db(O), 'links'), where('observerUid', '==', O))))).size, 1);
    await assertFails(getDocs(query(collection(db(X), 'links'), where('observerUid', '==', O))));
    await assertSucceeds(getDoc(doc(db(X), 'links', X)));
  });

  test('observer declines removal by clearing removalRequestedAt', async () => {
    await seedBase('removal_requested', { removalRequestedAt: Timestamp.now() });
    await assertSucceeds(updateDoc(doc(db(O), 'links', U), { status: 'active', removalRequestedAt: deleteField() }));
  });

  test('observer manages their own schedule on the user; user cannot delete it; user reads it', async () => {
    await seedBase('active');
    const sc = (uid) => doc(db(uid), 'users', U, 'schedules', 'sc1');
    const body = { createdBy: O, mode: 'full', oneTimeDate: '2030-01-01', startMin: 540, endMin: 600, tz: 'Asia/Dhaka', enabled: true };
    await assertSucceeds(setDoc(sc(O), body));
    await assertSucceeds(getDoc(sc(U)));
    await assertFails(deleteDoc(sc(U)));
    await assertSucceeds(updateDoc(sc(O), { enabled: false }));
    await assertSucceeds(deleteDoc(sc(O)));
  });
});

test('default deny for unknown paths', async () => {
  await assertFails(setDoc(doc(db(U), 'misc', 'x'), { a: 1 }));
  await assertFails(getDoc(doc(db(U), 'misc', 'x')));
  await assertFails(getDoc(doc(anon(), 'users', U)));
});
