package org.opengauss.smoke;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 事务行为验证用服务。 */
@Service
public class TxService {

    private final UserRepository repo;

    public TxService(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void saveThenThrow(String name, int age) {
        repo.save(new User(name, age));
        throw new RuntimeException("事务回滚验证");
    }
}
