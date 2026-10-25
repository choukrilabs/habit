const { test, describe, before, after, beforeEach } = require('node:test');
const assert = require('node:assert');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} = require('@firebase/rules-unit-testing');
const fs = require('node:fs');

let testEnv;

describe('Habit Radar Firestore Security Rules', () => {
  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: 'gen-lang-client-0244661994',
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8'),
        host: '127.0.0.1',
        port: 8085
      }
    });
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  test('denies unauthenticated access to habits and entries', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(unauthedDb.doc('users/alice/habits/habit_1').get());
    await assertFails(unauthedDb.collection('users/alice/habits').get());
    await assertFails(unauthedDb.doc('users/alice/entries/entry_1').get());
  });

  test('allows owner to create, read, list, update, and delete valid habit', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const habitRef = aliceDb.doc('users/alice/habits/habit_1');
    const now = new Date(Date.now() - 1000);

    await assertSucceeds(habitRef.set({
      userId: 'alice',
      title: 'Morning Meditation',
      category: 'Mindfulness',
      iconName: 'self_improvement',
      colorHex: '#B6A6E8',
      frequencyType: 'DAILY',
      targetPerDay: 1,
      isArchived: false,
      createdAt: now,
      updatedAt: now
    }));

    await assertSucceeds(habitRef.get());
    await assertSucceeds(aliceDb.collection('users/alice/habits').get());

    await assertSucceeds(habitRef.update({
      title: 'Evening Meditation',
      targetPerDay: 2,
      updatedAt: new Date(Date.now() - 500)
    }));

    await assertSucceeds(habitRef.delete());
  });

  test('denies cross-user access to another user habits and entries', async () => {
    const now = new Date(Date.now() - 5000);
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const adminDb = ctx.firestore();
      await adminDb.doc('users/alice/habits/habit_1').set({
        userId: 'alice',
        title: 'Secret Habit',
        category: 'General',
        iconName: 'check',
        colorHex: '#A8D8B9',
        frequencyType: 'DAILY',
        targetPerDay: 1,
        isArchived: false,
        createdAt: now,
        updatedAt: now
      });
    });

    const bobDb = testEnv.authenticatedContext('bob').firestore();
    await assertFails(bobDb.doc('users/alice/habits/habit_1').get());
    await assertFails(bobDb.collection('users/alice/habits').get());
    await assertFails(bobDb.doc('users/alice/habits/habit_1').delete());
  });

  test('denies habit creation with invalid schema or extra fields', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const now = new Date(Date.now() - 1000);

    // Extra field
    await assertFails(aliceDb.doc('users/alice/habits/habit_bad_1').set({
      userId: 'alice',
      title: 'Read 20 pages',
      category: 'Growth',
      iconName: 'book',
      colorHex: '#8EC5E8',
      frequencyType: 'DAILY',
      targetPerDay: 1,
      isArchived: false,
      extraField: 'injected',
      createdAt: now,
      updatedAt: now
    }));

    // Invalid frequencyType
    await assertFails(aliceDb.doc('users/alice/habits/habit_bad_2').set({
      userId: 'alice',
      title: 'Read 20 pages',
      category: 'Growth',
      iconName: 'book',
      colorHex: '#8EC5E8',
      frequencyType: 'YEARLY_INVALID',
      targetPerDay: 1,
      isArchived: false,
      createdAt: now,
      updatedAt: now
    }));
  });

  test('allows owner to create, update, list, and delete valid entry', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const entryRef = aliceDb.doc('users/alice/entries/entry_1');
    const now = new Date(Date.now() - 1000);

    await assertSucceeds(entryRef.set({
      userId: 'alice',
      habitId: 'habit_1',
      date: '2026-10-25',
      status: 'COMPLETED',
      count: 1,
      note: 'Felt great today!',
      createdAt: now,
      updatedAt: now
    }));

    await assertSucceeds(entryRef.get());
    await assertSucceeds(aliceDb.collection('users/alice/entries').get());

    await assertSucceeds(entryRef.update({
      count: 2,
      note: 'Double session!',
      updatedAt: new Date(Date.now() - 500)
    }));

    await assertSucceeds(entryRef.delete());
  });
});
