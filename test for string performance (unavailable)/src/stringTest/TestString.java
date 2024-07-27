package stringTest;

import java.util.Arrays;

import unsafeString.DirectCharArray;
import stringUtil.StringUtils;

public class TestString {

	String[] strings = new String[]{
			
			"policyID,statecode,county,eq_site_limit,hu_site_limit,fl_site_limit,fr_site_limit,tiv_2011,tiv_2012,eq_site_deductible,hu_site_deductible,fl_site_deductible,fr_site_deductible,point_latitude,point_longitude,line,construction,point_granularity",
			"119736,FL,CLAY COUNTY,498960,498960,498960,498960,498960,792148.9,0,9979.2,0,0,30.102261,-81.711777,Residential,Masonry,1",
			"448094,FL,CLAY COUNTY,1322376.3,1322376.3,1322376.3,1322376.3,1322376.3,1438163.57,0,0,0,0,30.063936,-81.707664,Residential,Masonry,3",
			"206893,FL,CLAY COUNTY,190724.4,190724.4,190724.4,190724.4,190724.4,192476.78,0,0,0,0,30.089579,-81.700455,Residential,Wood,1",
			"333743,FL,CLAY COUNTY,0,79520.76,0,0,79520.76,86854.48,0,0,0,0,30.063236,-81.707703,Residential,Wood,3",
			"172534,FL,CLAY COUNTY,0,254281.5,0,254281.5,254281.5,246144.49,0,0,0,0,30.060614,-81.702675,Residential,Wood,1",
			"785275,FL,CLAY COUNTY,0,515035.62,0,0,515035.62,884419.17,0,0,0,0,30.063236,-81.707703,Residential,Masonry,3",
			"995932,FL,CLAY COUNTY,0,19260000,0,0,19260000,20610000,0,0,0,0,30.102226,-81.713882,Commercial,Reinforced Concrete,1",
			"223488,FL,CLAY COUNTY,328500,328500,328500,328500,328500,348374.25,0,16425,0,0,30.102217,-81.707146,Residential,Wood,1",
			"433512,FL,CLAY COUNTY,315000,315000,315000,315000,315000,265821.57,0,15750,0,0,30.118774,-81.704613,Residential,Wood,1",
			"142071,FL,CLAY COUNTY,705600,705600,705600,705600,705600,1010842.56,14112,35280,0,0,30.100628,-81.703751,Residential,Masonry,1",
			"253816,FL,CLAY COUNTY,831498.3,831498.3,831498.3,831498.3,831498.3,1117791.48,0,0,0,0,30.10216,-81.719444,Residential,Masonry,1",
	
	};
	
	public static void main(String[] args) {
		
		new TestString();
		
	}
	
	
	public TestString() {
	

				
		DirectCharArray.getInstance().allocate(strings[0].length());
		
		TimeTracker split = new TimeTracker("SPLIT") {

			@Override
			public String[] execute(String str) {
				
				return str.split(",");
			}
			
		};
		
		TimeTracker stringutils = new TimeTracker("substring") {
			
			@Override
			public String[] execute(String str) {
				
				return StringUtils.split(str, ',');
			}
			
		};
	
		TimeTracker unsafeCopy = new TimeTracker("UnsafeCopy") {
			
			@Override
			public String[] execute(String str) {
				
				return StringUtils.unsafeCopy(str, ',');
			}
			
		};
		
		TimeTracker unsafecountBefore = new TimeTracker("unsafecountBefore") {
			
			@Override
			public String[] execute(String str) {
				
				return StringUtils.unsafecountBefore(str, ',');
			}
			
		};
		
		TimeTracker loop = new TimeTracker("loop") {
			
			@Override
			public String[] execute(String str) {
				
				return StringUtils.loop(str, ',');
			}
			
		};
		
		split.call(false);
		split.call(true);
		
		stringutils.call(false);
		stringutils.call(true);
		
		unsafeCopy.call(false);
		unsafeCopy.call(true);
		
		unsafecountBefore.call(false);
		unsafecountBefore.call(true);
		
		loop.call(false);
		loop.call(true);
		
		
		
	}
	
	abstract class TimeTracker {
		
		private String name;
		
		private static final int NUMBER = 1;
		
		public TimeTracker(String name) {
			this.name = name;
		}
		
		public void call(boolean show) {
			
			if(show)
			System.out.println("******************** " + name + " ***********************");
			
			long ini = System.nanoTime();
			String[] res = null;
			for(int i = 0; i < NUMBER; i++){
				for(String str : strings)
					res = execute(str);
			}
			long fim = System.nanoTime();
			
			if(show) {
				System.out.println(Arrays.toString(res));
				System.out.println("Milli: " + new Double(fim - ini) / 1000000);
				System.out.println("*******************************************");
				System.out.println("");
				}
		}
		
		public abstract String[] execute(String str);
		
		
		
	}
	
}
