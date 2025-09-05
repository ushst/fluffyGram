package org.ushastoe.fluffy.helpers;
import android.util.SparseIntArray;
public abstract class BaseIconSet {
  public SparseIntArray iconPack = new SparseIntArray();
  public Integer getIcon(Integer id) { return iconPack.get(id, id); }
}