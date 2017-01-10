package edu.bupt.wangfu.module.base;

import edu.bupt.wangfu.info.device.*;
import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.module.route.graph.Edge;

import java.util.Map;
import java.util.Set;

/**
 * Created by lenovo on 2016-6-22.
 */

public abstract class SysInfo {
	//本地属性
	public static String localMac;//本wsn节点所在计算机的mac地址
	public static String localGroupName;//本集群的名字
	public static String localAddr;//本系统的地址
	//	public static String multiAddr;//群内组播地址
	public static int sysPort;//控制消息端口号的起点
	public static int notifyPort;//转发消息端口号
	public static int tPort;//TCP端口号，接收本机内的发布者和订阅者的注册消息
	public static Controller localCtl;//节点控制器
	public static Controller groupCtl;//集群控制器
	//拓扑
	public static String localSwtId;//wsn连接的switch的id
	public static String portWsn2Swt;//wsn连接switch，switch上的的端口
	public static Set<Edge> groupEdges;//集群内所有swt连接的边的集合
	public static Map<String, Switch> outSwitches; //key是swtId，本集群所有拥有对外端口的swt的集合
	public static Set<Route> groupRoutes;//集群内所有计算过的路径
	public static Map<String, Host> hostMap;//当前集群所有host，key是mac
	public static Map<String, Switch> switchMap;//当前集群所有switch，key是id
	public static Map<String, GroupLink> nbrGrpLinks;//key是对面group的groupName，本集群与其邻居集群的连接情况
	public static Map<String, Group> allGroups;//key是groupName，当前网络中所有集群的连接情况
	public static Map<String, String> id2NameMap;//key是swt的openflow id，value是网桥的名字（如br0）
	//订阅信息
	public static Map<String, String> notifyTopicAddrMap;//主题树-->编码树，key是主题名，value是对应这个主题的v6地址
	public static Map<String, String> sysTopicAddrMap;//系统消息对应的编码，key是topic的名字，value是topic对应的v6地址
	//订阅表
	public static Map<String, String> localSubTopics;//本地订阅表，key是本地的订阅主题，value是接收端的webService地址
	public static Set<String> localPubTopic;//本地发布表，value是本地的发布主题
	public static Map<String, Set<String>> groupSubMap;//本集群的订阅信息，key是topic，value是swtId:port的集合
	public static Map<String, Set<String>> outerSubMap;//全网的订阅信息，key是topic，value是groupName的集合
	public static Map<String, Set<String>> groupPubMap;//本集群的发布信息，key是topic，value是swtId:port的集合
	public static Map<String, Set<String>> outerPubMap;//全网的发布信息，key是topic，value是groupName的集合
	public static Set<String> joinedSubTopics;//因聚合产生的订阅主题，它和unitedUnsubTopics都是为聚合做准备
	public static Set<String> joinedUnsubTopics;//因聚合而取消的订阅
	public static Map<String, Set<Flow>> notifyFlows;//key订阅的主题，value是这个主题对应的所有流表
	//管理员属性
	public static String adminAddr;//管理者的地址
	public static int adminPort;//管理者的地址
	//心跳管理器
	public static long helloTaskPeriod;//hello任务的执行周期
	public static long reHelloPeriod;//失效阀值的缺省值
	public static long helloPeriod;//发送hello的频率，这个间隔内应该足够完成re_hello等动作
	public static long refreshPeriod;//刷新集群内拓扑的频率
	public static long checkSplitPeriod;//进行当前订阅主题流量检查的频率，检查是否需要分裂为原订阅
	public static long nbrGrpExpiration;//邻居集群丢失时间的判断阈值
	public static int splitThreshold;//进行主题分裂的流量占比阈值的初始值
}
