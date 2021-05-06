package graphdeal;

/**
 * @Author Yuxuan Shi
 * @Date 11/27/2019
 * @Time 11:32 PM
 */
public class HopVD implements Comparable<HopVD>{
        public int v;
        private double dis;

        public double getDis(){
            return dis;
        }
        public HopVD(int v, double dis){
            this.v = v;
            this.dis = dis;
        }

        @Override
        public int compareTo(HopVD s2) {
            int result = Double.compare(this.dis, s2.dis);
            if (result == 0)
                result = Integer.compare(s2.v, this.v);
            return result;
        }
    }
