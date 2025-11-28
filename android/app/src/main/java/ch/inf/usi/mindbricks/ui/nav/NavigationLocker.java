// In a suitable package, e.g., ch.inf.usi.mindbricks.ui
package ch.inf.usi.mindbricks.ui.nav;

/**
 * An interface to be implemented by an Activity that needs to lock or unlock
 * its main navigation components (e.g., BottomNavigationView).
 */
public interface NavigationLocker {
    void setNavigationEnabled(boolean enabled);
}
