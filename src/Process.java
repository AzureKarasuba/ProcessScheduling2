import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class Process extends Object {
    int pid;

    int arrivalTime;

    int actualCurrBurstLeft;
    int estimatedCurrBurstLeft;

    ArrayList<Integer> burstTimes;
    ArrayList<Integer> burstTimesForEstimate;
    ArrayList<Integer> idleTimes;

    int currBurstIndex;
    int currIdleIndex;

    boolean firstTime;

    int nextEventTime;
    int timeSinceRunning;

    public int totalTime(){
        int total = 0;
        for (int b:
             burstTimes) {
            total +=b ;
        }
        for (int i:
             idleTimes) {
            total += i;
        }
        return total;
    }

    public int estimatedBurst(){
        int total = 0;

        if(currBurstIndex == 0){
            return 120;
        }
        for(int i = 0; i <= currBurstIndex - 1; i++){
            total += burstTimes.get(i);
        }

        //System.out.println("total: " + total);
        if(timeSinceRunning != 0){
            return total/(currBurstIndex) - timeSinceRunning;
        }
        return total/(currBurstIndex) == 0? 0 : total/(currBurstIndex);
    };

    public boolean burstFinished(){
        return currBurstIndex > (burstTimes.size()-1);
    }

    public boolean idleFinished(){
        return currIdleIndex > (idleTimes.size()-1);
    }

    public int nextEventTime(){
        return nextEventTime;
    }

    public boolean completed(){
        return currBurstIndex > (burstTimes.size()-1) && currIdleIndex > (idleTimes.size()-1);
    }

    Process(){
        burstTimes = new ArrayList<>();
        idleTimes = new ArrayList<>();

        burstTimesForEstimate = new ArrayList<>();

        firstTime = true;
    }

    public void load(String pName) throws FileNotFoundException {
        pid = Integer.parseInt(pName.substring(pName.indexOf('-') + 1,pName.indexOf('.')));

        File file = new File(pName);
        Scanner sc = new Scanner(file);

        String startLine = sc.nextLine().trim();
        arrivalTime = Integer.parseInt(startLine.split(" ")[1]);
        nextEventTime = arrivalTime;

        while(sc.hasNextLine()){
            String line = sc.nextLine().trim();
            //System.out.println(line);
            String state = line.split(" ")[0];
            if(Objects.equals(state, "end")){
                break;
            }else{
                if(Objects.equals(state, "B")){

                    burstTimes.add(Integer.parseInt(line.split(" ")[1]));
                    burstTimesForEstimate.add(Integer.parseInt(line.split(" ")[1]));

                }else{

                    idleTimes.add(Integer.parseInt(line.split(" ")[1]));

                }
            }
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(pid);
        sb.append("Burst times: " + burstTimes.size() + " ");
        for (int t:
             burstTimes) {
            sb.append(t + " ");
        }
        sb.append("Idle times: " + idleTimes.size()+" ");
        for (int t:
                idleTimes) {
            sb.append(t + " ");
        }
        return sb.toString();
    }
}
