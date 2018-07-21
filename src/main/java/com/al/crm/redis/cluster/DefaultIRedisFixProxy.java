package com.al.crm.redis.cluster;

import com.al.crm.nosql.cache.IRedisFix;
import com.al.crm.redis.route.ClientInstanceDiscover;

public class DefaultIRedisFixProxy extends AbstractIRedisFixProxy {

    private  ClientInstanceDiscover<IRedisFix> discover;

    /**
     * Sets the discover
     * <p>You can use getDiscover() to get the value of discover</p>
     *
     * @param discover discover
     */
    public void setDiscover(ClientInstanceDiscover<IRedisFix> discover) {
        this.discover = discover;
    }

    @Override
    protected IRedisFix determineCurrentRedisSource() {
        return discover.determineCurrentClientBean();
    }
}
