package edu.bupt.wangfu.mgr.route.graph;

import edu.bupt.wangfu.info.device.DevInfo;
import edu.bupt.wangfu.info.device.Switch;

import java.util.*;

/**
 * Created by lenovo on 2016/11/15.
 */
public class Dijkstra {
	public static List<String> dijkstra(String startSwtId, String endSwtId, Map<String, Switch> switchMap) {
		Set<Switch> op = new HashSet<>();
		//将所有switch存储在op集合中
		for (String st : switchMap.keySet()) {
			op.add(switchMap.get(st));
		}
		Switch startSwt = switchMap.get(startSwtId);
		Switch endSwt = switchMap.get(endSwtId);

		Set<Switch> open = new HashSet<>();
		open.addAll(op);
		op.remove(startSwt);
		Set<Switch> close = new HashSet<>();
		close.add(startSwt);
		//dis存储其他节点到startSwit节点的距离
		Map<String, Integer> dis = new HashMap<>();
		//path存储其他节点到startSwit经过的的节点
		Map<String, List<String>> path = new HashMap<>();
		//初始化dis，与startSwit节点不相邻则为-1
		for (Switch sw : open) {
			dis.put(sw.id, -1);
		}
		//设置与startSwit节点直接相邻的节点距离
		for (Switch sw : open) {
			for (DevInfo nbr : startSwt.neighbors.values()) {
				if (nbr instanceof Switch) {
					Switch swtNbr = (Switch) nbr;
					if (swtNbr.id.equals(sw.id)) {
//				dis.put(sw.id, startSwt.getNeighbors().get(sw.id).distance);
						//默认相邻节点间距离为1
						dis.put(sw.id, 1);
						path.put(sw.id, null);
					}
				}
			}
		}

		Switch nearest;
		while (!open.isEmpty()) {
			//查询距离startSwt最近的节点
			nearest = getNearestSwitch(dis, open);
			close.add(nearest);
			open.remove(nearest);
			//dis_1记录最近节点到startSwt的距离
			int dis_1 = dis.get(nearest.id);
			//更新dis中的距离信息
			for (Switch sw : open) {
				//dis_2当前节点到startSwt的距离
				int dis_2 = dis.get(sw.id);
				//dis_3记录当前节点到nearest节点的距离
				int dis_3;
				Map<String, DevInfo> neighbors = nearest.neighbors;
				if (neighbors.containsKey(sw.id)) {
//					dis_3 = neighbors.get(sw.id).distance;
					//默认相邻交换机距离为1
					dis_3 = 1;
				} else {
					dis_3 = -1;
				}

				if (dis_3 == -1) {
					//当前节点没有与nearest节点直接相邻，不操作
				} else if (dis_2 == -1 || dis_2 > dis_1 + dis_3) {
					//当前节点没有与start节点相邻或者通过nearest节点的距离更短，更新
					dis.put(sw.id, dis_1 + dis_3);
					//当前节点需通过nearest节点到达目标
					List<String> temp_1 = path.get(nearest.id);
					List<String> temp = new ArrayList<>();
					if (!(temp_1 == null)) {
						temp.addAll(temp_1);
					}
					temp.add(nearest.id);
					path.put(sw.id, temp);
				}
			}
		}
		return path.get(endSwt.id);
	}

	//返回dis中距离startSwt交换机最近的节点
	public static Switch getNearestSwitch(Map<String, Integer> dis, Set<Switch> open) {
		Switch res = null;
		int minDis = Integer.MAX_VALUE;
		//返回open集合中最小距离对应的节点
		for (Switch sw : open) {
			int distance = dis.get(sw.id);
			if (distance == -1) {
				//当前节点并未与startSwt相邻，不操作
			} else if (distance < minDis) {
				minDis = distance;
				res = sw;
			}
		}
		return res;
	}
}