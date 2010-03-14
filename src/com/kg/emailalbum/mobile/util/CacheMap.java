/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and Others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hisashi MIYASHITA - initial API and implementation
 *    Kentarou FUKUDA - initial API and implementation
 *    Kevin GAUDIN - full genericity + based on ConcurrentHashMap.
 *******************************************************************************/
package com.kg.emailalbum.mobile.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for cache map.
 */
public class CacheMap<K, V> extends ConcurrentHashMap<K, V> {
	private static final long serialVersionUID = 6681131647931821052L;

	private final int maxSize;
	private final int evictSize;

	private final LinkedList<K> accessList = new LinkedList<K>();

	/**
	 * Constructor of cache map. If the map exceed the maximum size, the
	 * key/value sets will be removed from map based on specified evict size.
	 * 
	 * @param maxSize
	 *            maximum size of the map
	 * @param evictSize
	 *            number of evict object
	 */
	public CacheMap(int maxSize, int evictSize) {
		this.maxSize = maxSize;
		this.evictSize = evictSize;
	}

	private void evict() {
		Iterator<K> it = accessList.iterator();
		for (int i = 0; i < evictSize; i++) {
			if (!it.hasNext())
				return;
			K key = it.next();
			this.remove(key);
			it.remove();
		}
	}

	private int searchAccessList(Object key) {
		return accessList.indexOf(key);
	}

	private void accessEntry(K key) {
		int idx = searchAccessList(key);
		if (idx >= 0) {
			accessList.remove(idx);
		}
		accessList.add(key);
	}

	@Override
	public V put(K key, V val) {
		if (size() >= maxSize)
			evict();
		accessEntry(key);
		V result = super.put(key, val);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ConcurrentHashMap#clear()
	 */
	@Override
	public void clear() {
		super.clear();
		accessList.clear();
	}

	@Override
	public V get(Object key) {
		accessEntry((K) key);
		return super.get(key);
	}

	// /**
	// * Search a key that starts with the specified prefix from the map, and
	// * return the value corresponding to the key.
	// *
	// * @param prefix
	// * target prefix
	// * @return the value whose key starts with prefix, or null if not
	// available
	// */
	// public Object matchStartsWith(String prefix) {
	// SortedMap<String, Object> smap = super.tailMap(prefix);
	// Object okey;
	// try {
	// okey = smap.firstKey();
	// } catch (NoSuchElementException e) {
	// return null;
	// }
	// if (!(okey instanceof String))
	// return null;
	// String key = (String) okey;
	//
	// if (!key.startsWith(prefix))
	// return null;
	// return super.get(key);
	// }
}