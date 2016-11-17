package edu.bupt.wangfu.mgr.route.graph;

import edu.bupt.wangfu.info.device.Group;

import java.util.*;

/**
 * Created by lenovo on 2016/11/17.
 */
public class GroupDijkstra {
	public static List<String> groupdijkstra(String startGrpName, String endGrpName, Map<String, Group> allGroups){
		Set<Group> op = new HashSet<>();
		Set<String> open = new HashSet<>();
		//將所有group存儲在op中，所有Group集群名存儲在open中
		for(String st : allGroups.keySet()){
			op.add(allGroups.get(st));
			open.add(allGroups.get(st).groupName);
		}
		Group startGrp = allGroups.get(startGrpName);
		Group endGrp = allGroups.get(endGrpName);

		op.remove(startGrp);
		Set<Group> close = new HashSet<>();
		close.add(startGrp);
		//distance存儲當前集群到starGrp集群距離
		Map<String, Integer> distance = new HashMap<>();
		//path存儲集群到達startGrp集群經過的集群
		Map<String, List<String>> path = new HashMap<>();

		//初始化distance，與startGrp集群不相鄰則設為-1
		for(String st : open){
			distance.put(st, -1);
		}
		//設置path信息
		for(String st : startGrp.dist2NbrGrps.keySet()){
			if(open.contains(st)){
				distance.put(st, startGrp.dist2NbrGrps.get(st));
				path.put(st, null);
			}
		}

		Group nearest = startGrp;
		while (nearest != endGrp){
			//查詢與startGrp距離最近的集群
			nearest = getNearestGroup(distance, op);
			op.remove(nearest);
			close.add(nearest);
			//dis_1記錄最近集群到startGrp集群的距離
			int dis_1 = distance.get(nearest.groupName);
			//更新distance中的信息
			for(Group gr : op){
				//dis_2存儲當前集群到startGrp集群的距離
				int dis_2 = distance.get(gr.groupName);
				//dis_3記錄當前集群到nearest集群的距離
				int dis_3 = 0;
				if(nearest.dist2NbrGrps.containsKey(gr.groupName)){
					dis_3 = nearest.dist2NbrGrps.get(gr.groupName);
				}

				if(dis_2 == -1 || dis_2 > dis_1 + dis_3){
					//当前集群没有与startGrp集群相邻或者通过nearest集群的距离更短，更新
					distance.put(gr.groupName, dis_1 + dis_3);


					//当前节点需通过nearest集群到达目标
					List<String> temp_1 = path.get(nearest.groupName);
					List<String> temp = new ArrayList<>();
					if (!(temp_1 == null)) {
						temp.addAll(temp_1);
					}
					temp.add(nearest.groupName);
					path.put(gr.groupName, temp);
				}
			}
		}
		ArrayList<String> across = new ArrayList<String>();
		across.add(startGrpName);
		if(!(path.get(endGrpName) == null)){
			across.addAll(path.get(endGrpName));
		}
		across.add(endGrpName);
		return across;
	}

	//返回distance中距离startGrp集群最近的集群
	public static Group getNearestGroup(Map<String, Integer> distance, Set<Group> op) {
		Group res = null;
		int minDis = Integer.MAX_VALUE;
		//返回op集合中最小距离对应的集群
		for (Group gr : op) {
			int dis = distance.get(gr.groupName);
			if (dis == -1) {
				//当前节点并未与startSwt相邻，不操作
			} else if (dis < minDis) {
				minDis = dis;
				res = gr;
			}
		}
		return res;
	}
}
