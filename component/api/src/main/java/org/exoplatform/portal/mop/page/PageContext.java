package org.exoplatform.portal.mop.page;

import java.util.List;

import org.exoplatform.portal.config.model.Page;

import lombok.Data;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@Data
public class PageContext {

  /** . */
  final PageKey key;

  /** The new state if any. */
  PageState     state;

  /** A data snapshot. */
  PageData      data;

  PageContext(PageData data) {
    this.key = data.key;
    this.state = null;
    this.data = data;
  }

  public PageContext(PageKey key, PageState state) {
    this.key = key;
    this.state = state;
    this.data = null;
  }

  /**
   * Returns the navigation key.
   *
   * @return the navigation key
   */
  public PageKey getKey() {
    return key;
  }

  /**
   * Returns the navigation state.
   *
   * @return the navigation state
   */
  public PageState getState() {
    if (state != null) {
      return state;
    } else if (data != null) {
      return data.state;
    } else {
      return null;
    }
  }

  /**
   * Updates the page state the behavior is not the same wether or not the page
   * is persistent: When the page is persistent, any state is allowed:
   * <ul>
   * <li>A non null state overrides the current persistent state.</li>
   * <li>The null state means to reset the state to the persistent state.</li>
   * <li>When the page is transient, only a non null state is allowed as it will
   * be used for creation purpose.</li>
   * </ul>
   *
   * @param state the new state
   * @throws IllegalStateException when the state is cleared and the navigation
   *           is not persistent
   */
  public void setState(PageState state) throws IllegalStateException {
    if (data == null && state == null) {
      throw new IllegalStateException("Cannot clear state on a transient page");
    }
    this.state = state;
  }

  public void update(Page page) throws NullPointerException {
    if (page == null) {
      throw new NullPointerException();
    }
    PageState s = getState();
    page.setTitle(s.getDisplayName());
    page.setDescription(s.getDescription());
    page.setFactoryId(s.getFactoryId());
    page.setShowMaxWindow(s.isShowMaxWindow());
    page.setHideSharedLayout(s.isHideSharedLayout());
    page.setShowSharedLayout(s.isShowSharedLayout());
    List<String> permisssions = s.getAccessPermissions();
    page.setAccessPermissions(permisssions == null ? null : permisssions.toArray(new String[permisssions.size()]));
    page.setEditPermission(s.getEditPermission());
  }
}
