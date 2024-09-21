package org.hibernate.dialect.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    public static SessionFactory getSessionFactory(Class<?>... classes) {
        Configuration configuration = new Configuration().configure();
        if (classes != null) {
            for (Class<?> c : classes) {
                configuration.addAnnotatedClass(c);
            }
        }
        return configuration.buildSessionFactory();
    }
}
