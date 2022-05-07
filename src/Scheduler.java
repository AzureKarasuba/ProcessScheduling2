import java.io.FileNotFoundException;
import java.util.*;

public class Scheduler {
    PriorityQueue<Process> readyQueue;
    PriorityQueue<Process> eventQueue;

    ArrayList<Integer> pidList;
    HashMap<Integer,Integer> pidToIndex;
    HashMap<Integer,Integer> indexToPid;

    boolean verbose = false;
    String algo;
    int quantum;

    int[] waitingTime;

    Scheduler(String algo){
        pidToIndex = new HashMap<>();
        pidList = new ArrayList<>();
        indexToPid = new HashMap<>();

        eventQueue = new PriorityQueue<>(new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                if(o1.nextEventTime < o2.nextEventTime){
                    return -1;
                }else if(o1.nextEventTime > o2.nextEventTime){
                    return 1;
                }else{
                    if(o1.pid < o2.pid){
                        return -1;
                    }else {
                        return 1;
                    }
                }
            }
        });
        this.algo = algo;
    }

    public void SchedulerReader(ArrayList<String> files) throws FileNotFoundException {
        for (String file:
             files) {
            Process p = new Process();
            //System.out.println(file);
            p.load(file);
            //System.out.println(p);
            eventQueue.add(p);
        }
    }

    public void SJR(){
        int curTime = 0;
        Process currentProcess = null;
        int currBurstTimeLeft = 0;
        int completed = 0;
        boolean isInIdle = false;
        boolean stateChange = false;
        boolean changeToIdle = false;

        int idleStart = 0;

        //SJR priority - the shortest estimate burst time, if tie then go with the smallest pid
        readyQueue = new PriorityQueue<>(new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                if(o1.estimatedBurst() < o2.estimatedBurst()){
                    return -1;
                }else if(o1.estimatedBurst() > o2.estimatedBurst()){
                    return 1;
                }else{
                    if(o1.pid < o2.pid){
                        return -1;
                    }else {
                        return 1;
                    }
                }
            }
        });

        int numOfProcesses =  eventQueue.size();
        waitingTime = new int[numOfProcesses];

        while(completed != numOfProcesses){
            //move process from eventQueue to readyQueue


            //System.out.println("here when time is " + curTime + "complete is + " + completed);
            while(eventQueue.peek() != null && eventQueue.peek().nextEventTime() <= curTime){
                Process p = eventQueue.remove();
                readyQueue.add(p);
                 //System.out.println("removed & added " + p.pid + " when time is " + curTime);
                 //System.out.println("nextBurstTime for " + p.pid + " : " + p.nextEventTime());
            }




            if( currentProcess != null /*&& currBurstTimeLeft == 0*/){
                if(readyQueue.peek()!= null &&
                        readyQueue.peek().estimatedBurst() < currentProcess.estimatedCurrBurstLeft) {
                    stateChange = true;
                    //print preempted end time
                    System.out.println(curTime);

                    //preempted switch between processes
                    if(currBurstTimeLeft != 0){ // current burst time gets preempted
                        //modify actual current burst time

                        currentProcess.burstTimes.set(currentProcess.currBurstIndex,
                                currentProcess.burstTimes.get(currentProcess.currBurstIndex) -
                                        currentProcess.timeSinceRunning);

                        currentProcess.timeSinceRunning = 0;
                        //System.out.println("current process " + currentProcess.pid  + " preempted" + " when time is " + curTime);
                        //System.out.println("running time left: " + currentProcess.estimatedBurst());

                        //ready starting from current time
                        currentProcess.nextEventTime = curTime;
                        //change to new process

                        Process preemptedP = currentProcess;
                        //eventQueue.add(currentProcess);
                        currentProcess = readyQueue.remove();
                        readyQueue.add(preemptedP);


                        currBurstTimeLeft = currentProcess.burstTimes.get(currentProcess.currBurstIndex);
                        //System.out.println("replaced by: " + currentProcess.pid + " with estimated b time: " + currentProcess.estimatedBurst());
                        if (verbose) {
                            System.out.println("\nCurrent time: " + curTime);
                            System.out.println("current burst index: " + currentProcess.currBurstIndex);
                            System.out.println("Estimated burst time: ");
                            for (Process P :
                                    readyQueue) {
                                System.out.println("Process " + P.pid + ": " + P.estimatedBurst());
                            }
                            for (Process P :
                                    eventQueue) {
                                System.out.println("Process " + P.pid + ": " + P.estimatedBurst());
                            }
                            System.out.println("Process " + currentProcess.pid + ": " + currentProcess.estimatedBurst());
                            //System.out.println(" Estimated burst time: " + currentProcess.estimatedBurst());
                        }
                        System.out.print(currentProcess.pid + " " + curTime + " ");
                    }else{ //preempted when time left = 0
                        //System.out.println(curTime);

                        currentProcess.currBurstIndex++;
                        currentProcess.timeSinceRunning = 0;
                        if(!currentProcess.completed()){

                            currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                            currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                            if(!currentProcess.completed()){
                                eventQueue.add(currentProcess);
                            }else{
                                //last is idle
                                waitingTime[pidToIndex.get(currentProcess.pid)] = curTime +
                                        currentProcess.idleTimes.get(currentProcess.idleTimes.size()-1)
                                        - currentProcess.arrivalTime - currentProcess.totalTime();
                                completed++;
                            }
                        }else{
                            //last is Burst
                            waitingTime[pidToIndex.get(currentProcess.pid)] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                        currentProcess = null;
                        //System.out.println("after set to null");
                        //System.out.println("complete is " + completed);

                        if(readyQueue.isEmpty() && !eventQueue.isEmpty()){
                            System.out.print("Idle " + curTime + " ");
                            changeToIdle = true;


                        }//if(readyQueue.isEmpty()
                    }

                }else if(currBurstTimeLeft == 0){ //not preempted but burst time ran out
                    //System.out.println("current burst time ran out for pid and not preempted = " + currentProcess.pid + "when time is " + curTime);
                    System.out.println(curTime);

                    currentProcess.currBurstIndex++;
                    currentProcess.timeSinceRunning = 0;
                    if(!currentProcess.completed()){

                        currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                        currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                        if(!currentProcess.completed()){
                            eventQueue.add(currentProcess);
                        }else{
                            //last is idle
                            waitingTime[pidToIndex.get(currentProcess.pid)] = curTime +
                                    currentProcess.idleTimes.get(currentProcess.idleTimes.size()-1)
                                    - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                    }else{
                        //last is Burst
                        waitingTime[pidToIndex.get(currentProcess.pid)] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                        completed++;
                    }
                    currentProcess = null;
                    //System.out.println("after set to null");
                    //System.out.println("complete is " + completed);

                    if(readyQueue.isEmpty() && !eventQueue.isEmpty()){
                        System.out.print("Idle " + curTime + " ");
                        changeToIdle = true;


                    }//if(readyQueue.isEmpty()
                }//else if(currBurstTimeLeft == 0)
            }

            //add new process from idle state
            if(currentProcess == null && !readyQueue.isEmpty()){

                //System.out.println("This should happen!!!");

                currentProcess = readyQueue.remove();
                currentProcess.estimatedCurrBurstLeft = currentProcess.estimatedBurst();

                currBurstTimeLeft = currentProcess.burstTimes.get(currentProcess.currBurstIndex);
                //System.out.println("curr pid " + currentProcess.pid);
                //System.out.println("curr index " + currentProcess.currBurstIndex);
                //System.out.println("currBurstTimeLeft " + currBurstTimeLeft);
                //print start time

                if(changeToIdle){
                    //print idle end time
                    System.out.println(curTime);
                    changeToIdle = false;
                }

                if(verbose){
                    System.out.println("\nCurrent time: " + curTime);
                    // System.out.println("current burst index: " +  currentProcess.currBurstIndex);
                    System.out.println("Estimated burst time: ");
                    for (Process P:
                         readyQueue) {
                        System.out.println("Process " + P.pid + ": " + P.estimatedBurst());
                    }
                    for (Process P:
                            eventQueue) {
                        System.out.println("Process " + P.pid + ": " + P.estimatedBurst());
                    }
                    System.out.println("Process " + currentProcess.pid + ": " + currentProcess.estimatedBurst());
                    //System.out.println(" Estimated burst time: " + currentProcess.estimatedBurst());
                }

                System.out.print(currentProcess.pid + " ");



                System.out.print(curTime+ " ");
                stateChange = false;

            }
            curTime++;
            currBurstTimeLeft--;
            if(currentProcess != null){
                currentProcess.timeSinceRunning++;
                currentProcess.estimatedCurrBurstLeft--;
                //System.out.println(currentProcess.pid);
             //   System.out.println(currBurstTimeLeft);
             //   System.out.println("time since runingL " + currentProcess.timeSinceRunning);
                //System.out.println(currentProcess.estimatedCurrBurstLeft);
            }


        }//while
        System.out.println("\nend");
        System.out.println("Waiting time:");
        for(int i = 0; i < numOfProcesses; i++){
            System.out.println("Process " + indexToPid.get(i) +" : " + waitingTime[i]);
        }
    }

    public void SJF(){
        int curTime = 0;
        Process currentProcess = null;
        int currBurstTimeLeft = 0;
        int completed = 0;
        boolean isInIdle = false;
        boolean stateChange = false;

        //SJF priority - the shortest estimate burst time, if tie then go with the smallest pid
        readyQueue = new PriorityQueue<>(new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                if(o1.estimatedBurst() < o2.estimatedBurst()){
                    return -1;
                }else if(o1.estimatedBurst() > o2.estimatedBurst()){
                    boolean answer = o1.estimatedBurst() == o2.estimatedBurst();
                    return 1;
                }else{
                    if(o1.pid < o2.pid){
                        return -1;
                    }else {
                        return 1;
                    }
                }
            }
        });

        int numOfProcesses =  eventQueue.size();
        waitingTime = new int[numOfProcesses];

        while(completed != numOfProcesses){
            //move process from eventQueue to readyQueue
            //System.out.println(completed);

            while(eventQueue.peek() != null && eventQueue.peek().nextEventTime() <= curTime){
                Process p = eventQueue.remove();
                readyQueue.add(p);
               // System.out.println("removed & added " + p.pid);
            }
            /*
            for (Process p:
                 eventQueue) {
                //System.out.println("a round");
                if(p.nextEventTime() == curTime){
                    eventQueue.remove(p);
                    readyQueue.add(p);
                    System.out.println("removed & added " + p.pid);
                }//if
            }//for

             */

            //System.out.println("current event size: " + eventQueue.size());

            if( currentProcess != null && currBurstTimeLeft == 0){ //current burst time ends
                //next burst time begins after current time + idle time if existing
                //print end time
                System.out.println(curTime);
                currentProcess.currBurstIndex++;
                //readyQueue.remove(currentProcess);
                stateChange = true;

                if(!currentProcess.completed()){

                    currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                    currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                    if(!currentProcess.completed()){
                        eventQueue.add(currentProcess);
                    }else{
                        waitingTime[pidToIndex.get(currentProcess.pid)] =
                                curTime + currentProcess.idleTimes.get(currentProcess.idleTimes.size()-1)
                                - currentProcess.arrivalTime - currentProcess.totalTime();
                        completed++;
                    }
                }else{
                    waitingTime[pidToIndex.get(currentProcess.pid)] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                    completed++;
                }

                currentProcess = null;

                if(readyQueue.isEmpty() && completed != numOfProcesses && !isInIdle){
                    System.out.print("Idle " + curTime + " ");
                    isInIdle = true;
                }
            }

            if(currentProcess == null && !readyQueue.isEmpty()){

                if(isInIdle){
                    System.out.println(curTime + " "); // end of idle
                    isInIdle = false;
                }

                currentProcess = readyQueue.remove();

                currBurstTimeLeft = currentProcess.burstTimes.get(currentProcess.currBurstIndex);
                //print start time


                if(verbose){
                    System.out.println("\nCurrent time: " + curTime);
                    // System.out.println("current burst index: " +  currentProcess.currBurstIndex);
                    System.out.println("Estimated burst time: ");
                    for (Process P:
                            readyQueue) {
                        System.out.println("Process " + P.pid + ": " + P.estimatedBurst());
                    }
                    for (Process P:
                            eventQueue) {
                        System.out.println("Process " + P.pid + ": " + P.estimatedBurst());
                    }
                    System.out.println("Process " + currentProcess.pid + ": " + currentProcess.estimatedBurst());
                    //System.out.println(" Estimated burst time: " + currentProcess.estimatedBurst());
                }

                System.out.print(currentProcess.pid + " " + curTime+ " ");

            }

            curTime++;
            currBurstTimeLeft--;
        }//while
        System.out.println("end");
        System.out.println("Waiting time:");
        for(int i = 0; i < numOfProcesses; i++){
            System.out.println("Process " + indexToPid.get(i) +" : " + waitingTime[i]);
        }

    }//SJF

    public void RR(int Q){
        int curTime = 0;
        Process currentProcess = null;
        int currentQuantumRemaining = 0;
        int completed = 0;
        boolean isInIdle = false;

        //RR priority - go with the smallest pid
        readyQueue = new PriorityQueue<>(new Comparator<Process>() { //use next time as priority
            @Override
            public int compare(Process o1, Process o2) {
                if(o1.nextEventTime < o2.nextEventTime){
                    return -1;
                }else if(o1.nextEventTime > o2.nextEventTime){
                    return 1;
                }else{
                    if(o1.pid < o2.pid){
                        return -1;
                    }else {
                        return 1;
                    }
                }
            }
        });

        int numOfProcesses =  eventQueue.size();
        waitingTime = new int[numOfProcesses];

        while(completed != numOfProcesses){
            //move process from eventQueue to readyQueue

            while(eventQueue.peek() != null && eventQueue.peek().nextEventTime() <= curTime){
                Process p = eventQueue.remove();
                readyQueue.add(p);
            }

            if( currentProcess != null){ //current process exists
                if(currentQuantumRemaining == 0){ // current Q ran out
                    System.out.println(curTime);
                    if(currentProcess.actualCurrBurstLeft == 0){ // current burst interval ends

                        if(!currentProcess.completed()){
                            currentProcess.currBurstIndex++;

                            if(currentProcess.currIdleIndex < currentProcess.idleTimes.size()){
                                currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                            }
                            currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                            if(!currentProcess.completed()){
                                eventQueue.add(currentProcess);
                            }else{
                                waitingTime[pidToIndex.get(currentProcess.pid)] = curTime
                                        + currentProcess.idleTimes.get(currentProcess.idleTimes.size()-1)
                                        - currentProcess.arrivalTime - currentProcess.totalTime();
                                completed++;
                            }
                        }else{
                            waitingTime[pidToIndex.get(currentProcess.pid)] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                        currentProcess = null;
                    }else { // Q ran out but current burst interval doesn't end
                        //subtract Q from current burst interval
                        currentProcess.burstTimes.set(currentProcess.currBurstIndex,
                                currentProcess.burstTimes.get(currentProcess.currBurstIndex) - Q) ;
                        currentProcess.nextEventTime = curTime; // use next time as priority
                        readyQueue.add(currentProcess);
                        //currentProcess = readyQueue.remove();

                        currentProcess = null;

                    }

                }else if(currentProcess.actualCurrBurstLeft == 0) {// actual burst time ran out
                    System.out.println(curTime);
                    currentProcess.currBurstIndex++;
                    if(!currentProcess.completed()){

                        if(currentProcess.currIdleIndex < currentProcess.idleTimes.size()){
                            currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                        }
                        currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                        if(!currentProcess.completed()){
                            eventQueue.add(currentProcess);
                        }else{
                            waitingTime[pidToIndex.get(currentProcess.pid)] = curTime
                                    + currentProcess.idleTimes.get(currentProcess.idleTimes.size()-1)
                                    - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                    }else{
                        waitingTime[pidToIndex.get(currentProcess.pid)] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                        completed++;
                    }
                    currentProcess = null;
                }

            }

            if(currentProcess == null && !readyQueue.isEmpty()){

                if(isInIdle){
                    System.out.println(curTime + " "); // end of idle
                    isInIdle = false;
                }

                currentProcess = readyQueue.remove();
                currentQuantumRemaining = Q;
                currentProcess.actualCurrBurstLeft = currentProcess.burstTimes.get(currentProcess.currBurstIndex);

                if(verbose){
                    System.out.println("\nCurrent time: " + curTime);
                }

                //print start time
                System.out.print(currentProcess.pid + " ");

                System.out.print(curTime+ " ");

            }else if(currentProcess == null && !isInIdle && completed != numOfProcesses){

                System.out.print("Idle " + curTime + " ");
                isInIdle = true;

            }

            curTime++;
            if(currentProcess != null){
                currentProcess.actualCurrBurstLeft--;
            }
            currentQuantumRemaining--;
        }//while
        System.out.println("end");
        System.out.println("Waiting time:");
        for(int i = 0; i < numOfProcesses; i++){
            System.out.println("Process " + indexToPid.get(i) +" : " + waitingTime[i]);
        }

    }//RR

    public static void main(String[] args) {
        try{
            Scheduler scheduler = new Scheduler(args[0]);
            ArrayList<String> files = new ArrayList<>();

            int count = -1;

            if(Objects.equals(args[0], "RR")){
                scheduler.algo = "RR";
                scheduler.quantum = Integer.parseInt(args[1]);
                if(Objects.equals(args[2], "verbose")){
                    scheduler.verbose = true;
                    count = 3;
                }else{
                    count = 2;
                }
            }else if(Objects.equals(args[0], "SJF")){
                scheduler.algo = "SJF";
                if(Objects.equals(args[1], "verbose")){
                    scheduler.verbose = true;
                    count = 2;
                }else{
                    count = 1;
                }
            }else{
                scheduler.algo = "SJR";
                if(Objects.equals(args[1], "verbose")){
                    scheduler.verbose = true;
                    count = 2;
                }else{
                    count = 1;
                }
            }

            //add files into ArrayList
            for(;count < args.length; count++){
                Integer pid = Integer.parseInt(args[count].substring(args[count].indexOf('-') + 1,args[count].indexOf('.')));
                scheduler.pidList.add(pid);
                files.add(args[count]);
            }

            //initialize hashmap from pid to index
            for(int i = 0; i < scheduler.pidList.size(); i++){
                scheduler.pidToIndex.put(scheduler.pidList.get(i),i);
                scheduler.indexToPid.put(i,scheduler.pidList.get(i));
            }

            System.out.println("start");
            scheduler.SchedulerReader(files);
            switch (scheduler.algo){
                case "RR":
                    scheduler.RR(scheduler.quantum);
                    break;
                case "SJR":
                    scheduler.SJR();
                    break;
                case "SJF":
                    scheduler.SJF();
                    break;
            }
        }catch (Exception e){
            System.out.println( e.getMessage() );
            e.printStackTrace();
        }//catch
    }//main
}


