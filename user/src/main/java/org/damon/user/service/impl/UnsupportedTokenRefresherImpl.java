package org.damon.user.service.impl;

import org.damon.common.BizException;
import org.damon.user.domain.UserIdentity;
import org.damon.user.service.IThirdPartyTokenRefresher;
import org.springframework.stereotype.Service;

@Service
public class UnsupportedTokenRefresherImpl implements IThirdPartyTokenRefresher {

    @Override
    public boolean supports(String provider) {
        return false;
    }

    @Override
    public TokenRefreshResult refresh(UserIdentity identity) {
        throw new BizException("Token refresh is not implemented for provider: " + identity.getProvider());
    }
}
