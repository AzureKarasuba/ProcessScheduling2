import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

public class Scheduler {
    PriorityQueue<Process> readyQueue;
    PriorityQueue<Process> eventQueue;

    boolean verbose = false;
    String algo;
    int quantum;

    int[] waitingTime;

    Scheduler(String algo){
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
            //System.out.println(completed);

            while(eventQueue.peek() != null && eventQueue.peek().nextEventTime() <= curTime){
                Process p = eventQueue.remove();
                readyQueue.add(p);
                // System.out.println("removed & added " + p.pid);
            }

            //System.out.println("current event size: " + eventQueue.size());

            if( currentProcess != null /*&& currBurstTimeLeft == 0*/){ //current burst time ends
                if(readyQueue.peek()!= null &&
                        readyQueue.peek().estimatedBurst() < currentProcess.estimatedCurrBurstLeft){
                    stateChange = true;

                    //preempted switch between processes
                    if(currBurstTimeLeft != 0){ // current burst time gets preempted
                        //modify actual current burst time

                        currentProcess.burstTimes.set(currentProcess.currBurstIndex,
                                currentProcess.burstTimes.get(currentProcess.currBurstIndex) -
                                        currentProcess.timeSinceRunning) ;

                        //ready starting from current time
                        currentProcess.nextEventTime = curTime;
                        //change to new process
                        eventQueue.add(currentProcess);
                        currentProcess = readyQueue.remove();
                    }else{
                        //current burst time ran out
                        currentProcess.timeSinceRunning = 0;
                        currentProcess.currBurstIndex++;


                        if(!currentProcess.completed()){

                            currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                            currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                            if(!currentProcess.completed()){
                                eventQueue.add(currentProcess);
                            }else{
                                waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                                completed++;
                            }
                        }else{
                            waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                        currentProcess = readyQueue.remove();


                    }
                    if(readyQueue.isEmpty()){
                        changeToIdle = true;
                    }
                }else if(currBurstTimeLeft == 0){ //not preempted but burst time ran out

                    stateChange = true;

                    currentProcess.currBurstIndex++;
                    currentProcess.timeSinceRunning = 0;
                    if(!currentProcess.completed()){

                        currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                        currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                        if(!currentProcess.completed()){
                            eventQueue.add(currentProcess);
                        }else{
                            waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                    }else{
                        waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                        completed++;
                    }
                    currentProcess = null;

                    if(readyQueue.isEmpty()){
                        changeToIdle = true;
                    }
                }


            }

            if(stateChange){
                System.out.println(curTime);
            }else if(currentProcess == null && !readyQueue.isEmpty() && curTime != idleStart){
                System.out.println(curTime);
            }

            //readyQueue.remove(currentProcess);

            if(changeToIdle && completed !=numOfProcesses){

                System.out.print("Idle " + curTime + " ");
                changeToIdle = false;
                stateChange = false;
            }

            if(currentProcess == null && !readyQueue.isEmpty()){


                currentProcess = readyQueue.remove();
                currentProcess.estimatedCurrBurstLeft = currentProcess.estimatedBurst();

                currBurstTimeLeft = currentProcess.burstTimes.get(currentProcess.currBurstIndex);
                //print start time
                System.out.print(currentProcess.pid + " ");

                if(verbose){
                    System.out.print("current time: " + curTime);
                   // System.out.println("current burst index: " +  currentProcess.currBurstIndex);
                    System.out.println(" Estimated burst time: " + currentProcess.estimatedBurst());
                }

                System.out.print(curTime+ " ");
                stateChange = false;

            }

            curTime++;
            currBurstTimeLeft--;
            if(currentProcess != null){
                currentProcess.timeSinceRunning++;
                currentProcess.estimatedCurrBurstLeft--;
            }

        }//while
        System.out.println("end");
        System.out.println("Waiting time:");
        for(int i = 0; i < numOfProcesses; i++){
            System.out.println("Process " + (i+1) +" : " + waitingTime[i]);
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
                    boolean answer = o1.estimatedBurst() == o2.estimatedBurst();
                    //System.out.println( answer );
                    //System.out.println("compare estBurst: " + o1.pid + ":" +o1.estimatedBurst() + " with "
                            //+ o2.pid + ":" +o2.estimatedBurst() + " ,put " + o1.pid + " in front");
                    return -1;
                }else if(o1.estimatedBurst() > o2.estimatedBurst()){
                    boolean answer = o1.estimatedBurst() == o2.estimatedBurst();
                    //System.out.println( answer );
                    //System.out.println("compare estBurst: " + o1.pid + ":" +o1.estimatedBurst() + " with "
                           // + o2.pid + ":" +o2.estimatedBurst() + " ,put " + o2.pid + " in front");
                    return 1;
                }else{
                    if(o1.pid < o2.pid){
                       // System.out.println("compare pid: " + o1.pid + " with " + o2.pid + " ,put " + o1.pid + " in front");
                        return -1;
                    }else {
                      //  System.out.println("compare pid: " + o1.pid + " with " + o2.pid + " ,put " + o2.pid + " in front");
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
                        waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                        completed++;
                    }
                }else{
                    waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
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
                System.out.println(currentProcess.pid);

                if(verbose){
                    System.out.print("current time: " + curTime);
                    System.out.println(" Estimated burst time: " + currentProcess.estimatedBurst());
                }

                System.out.print(curTime+ " ");

            }

            curTime++;
            currBurstTimeLeft--;
        }//while
        System.out.println("end");
        System.out.println("Waiting time:");
        for(int i = 0; i < numOfProcesses; i++){
            System.out.println("Process " + (i+1) +" : " + waitingTime[i]);
        }

    }//SJF

    public void RR(int Q){
        int curTime = 0;
        Process currentProcess = null;
        int currentQuantumRemaining = 0;
        int completed = 0;
        boolean isInIdle = false;
        boolean stateChange = false;
        boolean repeat = false;

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
                                waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                                completed++;
                            }
                        }else{
                            waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
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
                    currentProcess.currBurstIndex++;
                    if(!currentProcess.completed()){

                        if(currentProcess.currIdleIndex < currentProcess.idleTimes.size()){
                            currentProcess.nextEventTime = curTime + currentProcess.idleTimes.get(currentProcess.currIdleIndex);
                        }
                        currentProcess.currIdleIndex = currentProcess.currBurstIndex;
                        if(!currentProcess.completed()){
                            eventQueue.add(currentProcess);
                        }else{
                            waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
                            completed++;
                        }
                    }else{
                        waitingTime[currentProcess.pid-1] = curTime - currentProcess.arrivalTime - currentProcess.totalTime();
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
            System.out.println("Process " + (i+1) +" : " + waitingTime[i]);
        }

    }//RR

    public static void main(String[] args) {
        try{
            Scheduler scheduler = new Scheduler(args[0]);
            ArrayList<String> files = new ArrayList<>();

            int count;

            if(args[0] =="RR"){
                scheduler.algo = "RR";
                scheduler.quantum = Integer.parseInt(args[1]);
                if(Objects.equals(args[2], "verbose")){
                    scheduler.verbose = true;
                    count = 3;
                }else{
                    count = 2;
                }
            }else if(args[0] == "SJF"){
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
                files.add(args[count]);
            }
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


