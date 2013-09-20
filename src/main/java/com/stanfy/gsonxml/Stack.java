package com.stanfy.gsonxml;

/**
 * Array-based stack.
 * @param <T> element type
 */
final class Stack<T> {

  /** Size. */
  private Object[] array = new Object[32];
  /** Stack size. */
  private int size = 0;

  @SuppressWarnings("unchecked")
  public T peek() {
    return (T) array[size - 1];
  }

  public int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  public T get(final int pos) {
    return (T) array[pos];
  }

  public void drop() {
    size--;
  }

  public int cleanup(final int count) {
    return cleanup(count, size);
  }

  public int cleanup(final int count, final int oldStackSize) {
    int curStackSize = size;
    if (oldStackSize < curStackSize) {
      for (int i = oldStackSize; i < curStackSize; i++) {
        array[i - count] = array[i];
      }
      size -= count;
    } else {
      size -= count - oldStackSize + curStackSize;
    }
    if (size < 0) { size = 0; }
    return oldStackSize - count;
  }

  public void fix(final T check) {
    size--;
    if (size > 0 && array[size - 1] == check) {
      size--;
    }
  }

  private void ensureStack() {
    if (size == array.length) {
      final Object[] newStack = new Object[size * 2];
      System.arraycopy(array, 0, newStack, 0, size);
      array = newStack;
    }
  }

  public void push(final T value) {
    ensureStack();
    array[size++] = value;
  }
  public void pushAt(final int position, final T scope) {
    int pos = position;
    if (pos < 0) { pos = 0; }
    ensureStack();
    for (int i = size - 1; i >= pos; i--) {
      array[i + 1] = array[i];
    }
    array[pos] = scope;
    size++;
  }



  @Override
  public String toString() {
    StringBuilder res = new StringBuilder();
    for (int i = 0; i < size; i++) {
      res.append(array[i]).append('>');
    }
    if (res.length() > 0) {
      res.delete(res.length() - 1, res.length());
    }
    return res.toString();
  }

}
