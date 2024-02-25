package com.xcpower.im.route.strategy.consistenthash;

import com.xcpower.im.enums.UserErrorCode;
import com.xcpower.im.exception.ApplicationException;

import java.util.SortedMap;
import java.util.TreeMap;


/**
 * 使用 TreeMap 实现一致性hash
 */
public class TreeMapConsistentHash extends AbstractConsistentHash {

    private TreeMap<Long, String> treeMap = new TreeMap<>();

    private static final int NODE_SIZE = 2;  // 每个节点再注册虚拟节点的数量

    @Override
    protected void add(long key, String value) {
        for (int i = 0; i < NODE_SIZE; i++) {
            treeMap.put(super.hash("node" + key + i), value);
        }
        treeMap.put(key, value);
    }

    @Override
    protected String getFirstNodeValue(String value) {

        Long hash = super.hash(value);
        SortedMap<Long, String> last = treeMap.tailMap(hash);
        if (!last.isEmpty()) {
            return last.get(last.firstKey());
        }

        if (treeMap.size() == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }

        return treeMap.firstEntry().getValue();
    }

    @Override
    protected void processBefore() {
        treeMap.clear(); // 清空是因为，可能出现添加或删除服务的现象
    }
}
