package com.rotor.core.interfaces;

import java.util.HashMap;

public interface RScreen {

    boolean isActive();

    boolean addPath(String path, Object obj);

    boolean removePath(String path);

    boolean hasPath(String path);

    HashMap<String, Object> holders();

    void connected();

    void disconnected();

}
