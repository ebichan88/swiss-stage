import { setupServer } from 'msw/node';

/** APIモックはMSWで行う(09_test_strategy.md §4: fetchの手モック禁止)。ハンドラは各テストで登録する */
export const server = setupServer();

export { apiError, apiSuccess } from './apiResponse';
