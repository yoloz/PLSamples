package com.util;

import java.net.URL;
import java.net.URLClassLoader;

public class ProxyClassLoader extends URLClassLoader {


    public ProxyClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    public synchronized Class loadClass(String clazz) throws ClassNotFoundException {
        Class c = findLoadedClass(clazz);
        if (c == null) {
            try {
                c = findClass(clazz);
            } catch (Exception e) {
                c = this.getParent().loadClass(clazz);
            }
        }
        return c;
    }
}
