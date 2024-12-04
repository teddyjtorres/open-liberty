/**
 *
 */
package test.jakarta.data.datastore.ejb;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Startup
@Singleton
public class StartupSingletonEJB {

    @Inject
    EJBModuleDSDRepo repo;

    //@PostConstruct //TODO re-enable after Data Repositories can be used during App start.
    public void init() {
        repo.acquire(0);
    }
}
