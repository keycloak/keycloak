package org.keycloak.models.jpa;

import org.hibernate.exception.ConstraintViolationException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelDuplicateException;

import javax.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistenceExceptionConverter implements InvocationHandler {

    private EntityManager em;

    public static EntityManager create(EntityManager em) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, new PersistenceExceptionConverter(em));
    }

    private PersistenceExceptionConverter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(em, args);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (c.getCause() != null && c.getCause() instanceof ConstraintViolationException) {
                throw new ModelDuplicateException(c);
            } else {
                throw new ModelException(c);
            }
        }
    }

}
