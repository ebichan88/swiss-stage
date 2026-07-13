/** GET /auth/me のレスポンス(backend: AuthController.MeDto) */
export interface Me {
  sub: string;
  name: string;
}
