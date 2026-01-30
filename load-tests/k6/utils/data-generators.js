import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const EXPENSE_DESCRIPTIONS = [
    'Dinner at restaurant',
    'Grocery shopping',
    'Uber ride',
    'Movie tickets',
    'Coffee',
    'Lunch',
    'Gas station',
    'Hotel booking',
    'Flight tickets',
    'Concert tickets',
    'Utilities bill',
    'Internet bill',
    'Phone bill',
    'Rent payment',
    'Home supplies',
    'Office supplies',
    'Birthday gift',
    'Party supplies',
    'Taxi fare',
    'Train tickets',
];

const CATEGORIES = [
    'FOOD',
    'TRANSPORT',
    'ENTERTAINMENT',
    'SHOPPING',
    'UTILITIES',
    'RENT',
    'TRAVEL',
    'OTHER',
];

const SPLIT_TYPES = ['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES'];

const GROUP_NAMES = [
    'Apartment',
    'Trip to Paris',
    'Office Lunch',
    'Weekend Getaway',
    'Roommates',
    'Family Vacation',
    'Road Trip',
    'Birthday Party',
    'Wedding',
    'Holiday Trip',
];

export function generateExpenseData(groupId, participants) {
    const amount = randomIntBetween(5, 500);
    const splitType = randomItem(SPLIT_TYPES);
    
    let participantData = participants.map(p => ({ userId: p }));
    
    if (splitType === 'PERCENTAGE') {
        const percentage = Math.floor(100 / participants.length);
        const remainder = 100 - (percentage * participants.length);
        participantData = participants.map((p, i) => ({
            userId: p,
            percentage: i === 0 ? percentage + remainder : percentage,
        }));
    } else if (splitType === 'EXACT') {
        const perPerson = Math.floor(amount / participants.length);
        const remainder = amount - (perPerson * participants.length);
        participantData = participants.map((p, i) => ({
            userId: p,
            amount: i === 0 ? perPerson + remainder : perPerson,
        }));
    } else if (splitType === 'SHARES') {
        participantData = participants.map(p => ({
            userId: p,
            shares: randomIntBetween(1, 3),
        }));
    }

    return {
        description: randomItem(EXPENSE_DESCRIPTIONS),
        amount: amount.toFixed(2),
        currency: 'USD',
        category: randomItem(CATEGORIES),
        groupId: groupId,
        splitType: splitType,
        participants: participantData,
        date: new Date().toISOString().split('T')[0],
    };
}

export function generateGroupName() {
    return `${randomItem(GROUP_NAMES)} ${randomIntBetween(1, 9999)}`;
}

export function generateSettlementData(groupId, toUserId, amount) {
    return {
        groupId: groupId,
        toUserId: toUserId,
        amount: amount,
        paymentMethod: randomItem(['CASH', 'VENMO', 'PAYPAL', 'ZELLE', 'BANK_TRANSFER']),
        note: 'Settlement payment',
    };
}

export function randomAmount(min = 1, max = 1000) {
    return (Math.random() * (max - min) + min).toFixed(2);
}

export function randomDelay(min = 1, max = 3) {
    return randomIntBetween(min, max);
}
