package dev.local.ideavim.strictime;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("org.fcitx.Fcitx.Controller1")
interface FcitxController extends DBusInterface {
    @DBusMemberName("State")
    int state() throws DBusException;

    @DBusMemberName("Deactivate")
    void deactivate() throws DBusException;
}
