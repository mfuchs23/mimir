package org.dbdoclet.mimir;

public interface IVisitor<T> {

	public default void before(T obj) {};
	public void accept(T obj);
	public default void after(T obj) {};
	public boolean isCancelled();
}
