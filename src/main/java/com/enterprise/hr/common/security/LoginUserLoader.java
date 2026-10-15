package com.enterprise.hr.common.security;

/**
 * 登录用户装载器：由令牌中的用户 ID 还原完整的 {@link LoginUser}（含角色与数据范围）。
 *
 * <p>抽成接口是为了让 {@link AuthInterceptor}（common 层）不直接依赖 system 模块的 Mapper，
 * 保持 common 层零业务依赖，便于后续替换实现（如加缓存、接外部统一认证）。
 *
 * @author Enterprise HR Team
 */
public interface LoginUserLoader {

    /**
     * 装载登录用户。
     *
     * @param userId sys_user.id
     * @return 登录用户；用户不存在、已逻辑删除时返回 {@code null}（调用方按 401 处理）
     */
    LoginUser load(Long userId);
}
