package edu.bupt.wangfu.module.route.graph;

import edu.bupt.wangfu.info.device.Group;

import java.util.*;

/**
 * Created by lenovo on 2017/1/12.
 */
public class shortestPath2Graph {
	public static List<String> shortestPath2Graph(String newlyAddedGrp, String topic, Map<String, Set<String>> outerSubMap, Map<String, Set<String>> outerPubMap, Map<String, Group> allGroups){
		//select存储订阅、发布topic主题的所有group名称
		Set<String> select = new TreeSet<String>();
		Set<String> outerSubGroup = outerSubMap.get(topic);
		Set<String> outerPubGroup = outerPubMap.get(topic);
		if(outerPubGroup != null)
			select.addAll(outerPubGroup);
		if(outerSubGroup != null)
			select.addAll(outerSubGroup);

		//当前集群在发布、订阅该主题集群中，最短距离为0
		if(select.isEmpty()){
			System.out.println("当前主题错误！");
			return null;
		}
		else if(select.contains(newlyAddedGrp)){
			System.out.println("当前集群为发布/订阅集群");
			return null;
		}
		else{
			String shortestGroup = null;
			int dis = Integer.MAX_VALUE;
			for(String st : select){
				int temp = distance(newlyAddedGrp, st, allGroups);
				if(temp < dis){
					dis = temp;
					shortestGroup = st;
				}
			}
			List<String> across = GroupDijkstra.groupdijkstra(newlyAddedGrp, shortestGroup, allGroups);
			return across;
		}
	}

	public static int distance(String startGrpName, String endGrpName, Map<String, Group> allGroups) {
		Set<Group> op = new TreeSet<>();
		Set<String> open = new TreeSet<>();
		//将所有group存储在op中，所有Group集群名存储在open中
		for (String st : allGroups.keySet()) {
			op.add(allGroups.get(st));
			open.add(allGroups.get(st).groupName);
		}
		Group startGrp = allGroups.get(startGrpName);
		Group endGrp = allGroups.get(endGrpName);

		op.remove(startGrp);
		Set<Group> close = new TreeSet<>();
		close.add(startGrp);
		//distance存储当前集群到starGrp集群距离
		Map<String, Integer> distance = new TreeMap<>();
		//path存储集群到达startGrp集群经过的集群
		Map<String, List<String>> path = new TreeMap<>();

		//初始化distance，与startGrp集群不相邻则设置为-1
		for (String st : open) {
			distance.put(st, -1);
		}
		//设置path信息
		for (String st : startGrp.dist2NbrGrps.keySet()) {
			if (open.contains(st)) {
				distance.put(st, startGrp.dist2NbrGrps.get(st));
				path.put(st, null);
			}
		}

		Group nearest = startGrp;
		while (nearest != endGrp) {
			//查询与startGrp距离最近的集群
			nearest = getNearestGroup(distance, op);
			op.remove(nearest);
			close.add(nearest);

			//dis_1记录最近集群到startGrp集群的距离
			int dis_1 = 0;
			if(distance.get(nearest.groupName) == null){
				return 0;
			}
			else{
				dis_1 = distance.get(nearest.groupName);
			}

			//更新distance中的信息
			for (Group gr : op) {
				//dis_2存储当前集群到startGrp集群的距离
				int dis_2 = distance.get(gr.groupName);
				//dis_3记录当前集群到nearest集群的距离
				int dis_3 = -1;
				if (nearest.dist2NbrGrps.containsKey(gr.groupName)) {
					dis_3 = nearest.dist2NbrGrps.get(gr.groupName);
				}

				if (dis_3 == -1) {
				} else if (dis_2 == -1 || dis_2 > dis_1 + dis_3) {
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
		return distance.get(endGrpName);
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

	public static void main(String[] args){
		Map<String, Group> allGroups = new HashMap<>();
		Group group_1 = new Group("1");
		allGroups.put("1", group_1);
		Group group_2 = new Group("2");
		allGroups.put("2", group_2);
		Group group_3 = new Group("3");
		allGroups.put("3", group_3);
		Group group_4 = new Group("4");
		allGroups.put("4", group_4);
		Group group_5 = new Group("5");
		allGroups.put("5", group_5);
		Group group_6 = new Group("6");
		allGroups.put("6", group_6);
		Group group_7 = new Group("7");
		allGroups.put("7", group_7);

		group_1.dist2NbrGrps.put("2", 1);
		group_1.dist2NbrGrps.put("4", 1);
		group_2.dist2NbrGrps.put("1", 1);
		group_2.dist2NbrGrps.put("3", 1);
		group_2.dist2NbrGrps.put("4", 1);
		group_3.dist2NbrGrps.put("2", 1);
		group_3.dist2NbrGrps.put("5", 1);
		group_4.dist2NbrGrps.put("1", 1);
		group_4.dist2NbrGrps.put("2", 1);
		group_4.dist2NbrGrps.put("6", 1);
		group_5.dist2NbrGrps.put("3", 1);
		group_5.dist2NbrGrps.put("6", 1);
		group_6.dist2NbrGrps.put("4", 1);
		group_6.dist2NbrGrps.put("5", 1);
		group_6.dist2NbrGrps.put("7", 1);
		group_7.dist2NbrGrps.put("6", 1);


		Set<String> set_1 = new HashSet<String>();
		set_1.add("1");
		set_1.add("2");
		set_1.add("3");
		Set<String> set_2 = new HashSet<String>();
		set_2.add("1");
		set_2.add("4");
		Map<String, Set<String>> outerSubMap = new HashMap<>();
		Map<String, Set<String>> outerPubMap = new HashMap<>();
		outerSubMap.put("lalala", set_1);
		outerPubMap.put("lalala", set_2);
		while(true){
			System.out.println("请输入当前集群：");
			Scanner in = new Scanner(System.in);
			String newlyAddedGrp = in.next();
			System.out.println("请输入主题名称：");
			String topic = in.next();
			List<String> across = new LinkedList<>();
			across = shortestPath2Graph(newlyAddedGrp, topic, outerSubMap, outerPubMap, allGroups);
			if(across == null){
				System.out.println("null");
			}
			else
				System.out.println(across);
		}
	}
}
