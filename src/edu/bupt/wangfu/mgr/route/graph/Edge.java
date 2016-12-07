package edu.bupt.wangfu.mgr.route.graph;

public class Edge implements Comparable {
	public String startPort;
	public String finishPort;
	public String topic;
	private String start;
	private String finish;
	private int value;

	public Edge() {

	}

	public Edge(String start, String finish, int value) {
		//认为排序，将边的起始点设置为ASCII码较小值
		if (start.compareTo(finish) < 0) {
			this.start = start;
			this.finish = finish;
			this.value = value;
		} else {
			this.start = finish;
			this.finish = start;
			this.value = value;
		}
	}

	@Override
	public String toString() {
		return "Edge{" +
				"start='" + start + '\'' +
				", startPort='" + startPort + '\'' +
				", finish='" + finish + '\'' +
				", finishPort='" + finishPort + '\'' +
				", value=" + value +
				'}';
	}

	public String getStart() {
		return this.start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getFinish() {
		return this.finish;
	}

	public void setFinish(String finish) {
		this.finish = finish;
	}

	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public void sequence() {
		if (this.start.compareTo(this.finish) > 0) {
			String temp;
			temp = this.start;
			this.start = this.finish;
			this.finish = temp;
		}
	}

	//实现comparable接口，Edge可调用sort升序排列
	@Override
	public int compareTo(Object arg0) {
		Edge other = (Edge) arg0;
		//根据边的值确定顺序，同一条边值一定相同，边的值相同未必是同一条边
		if (this.value > other.value) return 1;
		else if (this.value == other.value) {
			if ((this.start.equals(other.start) && this.finish.equals(other.finish))) {
				return 0;
			} else if (this.start.compareTo(other.start) == 0) return this.finish.compareTo(other.finish);
			else return this.start.compareTo(other.start);
		} else return -1;
	}
}
