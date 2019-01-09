package Runner;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import gnu.io.SerialPort;
import utils.SensorCmdHandler;
import utils.SerialPortManager;
import Sensor.Simulation_Task;

/**
 * SimulatorRunner 类用于模拟传感器工作模式，包括传感器的发送采集数据，接受指令，
 * 停止仪器。该程序主要工作模式为读取指定文件（数据），连续循环向指定的串口发送数据，同时 在接收到仪器命令时，做出相应的工作。
 * 
 * @author Yibing Zhang
 */
public class SimulatorRunner
{
    private static Queue<String>   queue      = new LinkedBlockingQueue<String>();// 存储接收到的命令
    private static SerialPort      port;                                          // 串口
    private static Simulation_Task task;                                          // 模拟器数据发送模块
    private static String          portName;                                      // 串口号
    private static String          sensorName;                                    // 模拟器名称，即模拟传感器名称
    private static String          fileName;                                      // 模拟器数据文件名称
    private static int             baudRate;                                      // 波特率
    private static int             fileType;                                      // 文件类型
    private static String          currentCMD = "";                               // 当前命令
    private static long            interval   = 300;                              // 采样间隔


    /**
     * main method用于启动该程序，args后分别为串口号,任务名（模拟仪器的名字），数据文件的名称和模特率
     * 
     * @param args
     *            args[0] 串口号 args[1] 模拟器名称 args[2] 数据文件名称 args[3] 波特率 args[4]
     *            文件类别：1为非2进制文件 2 为2进制文件
     */
    public static void main(String[] args)
    {
        if (args.length != 5)
        {
            System.err.println("Invalid parameters. Try again!\ne.g java -jar simulator.jar COM2 CTD ctd.dat 9600 1");
            System.exit(0);
        }
        portName = args[0];
        sensorName = args[1];
        fileName = args[2];
        baudRate = Integer.parseInt(args[3]);
        fileType = Integer.parseInt(args[4]);
        port = SerialPortManager.openPort(portName, baudRate);
        generateListener(port);
        task = new Simulation_Task(sensorName, port, fileName, fileType, interval);
        // Thread task_Thread = new Thread(task);
        // 运行数据发送
        // task_Thread.start();
        System.out.println("Simulator starts");
        new Command_Thread().run();
    }


    /**
     * Command_Thread 主要用于处理模拟器接受命令。不断循环查看阻塞queue里是否有命令，
     * 当接受到命令时，queue里会出现命令，遂立即对命令做出反应， 当无法识别当前命令时，拒绝该命令。
     * 
     * @author Yibing Zhang
     */
    public static class Command_Thread extends Thread
    {
        @Override
        public void run()
        {
            while (true)
            {
                while (queue.size() > 0)
                {
                    String content = queue.remove();
                    System.out.println("About to respond cmd: " + content);
                    SensorCmdHandler handler = new SensorCmdHandler(content);
                    // 设置handler的参数
                    handler.iniHandler(task, port, fileName, fileType,interval);
                    // 对命令做出反应
                    task = handler.doWork();
                    interval=handler.getInterval();
                }
            }
        }
    }


    /**
     * generateListener 对指定串口添加事件监听器（此处用了DataAviablerListener），即当该串口接
     * 收到命令时，立刻从该串口读取命令，并且添加到 阻塞（Blocking）的queue里，Command_Thread不断循环，
     * 判断queue里是否有命令，有命令即作相应处理
     * 
     * @param port
     */
    private static void generateListener(SerialPort port)
    {
        SerialPortManager.addListener(port, new SerialPortManager.DataAvailableListener() {
            @Override
            public void dataAvailable()
            {
                byte[] data = null;
                try
                {
                    if (port == null)
                    {
                        System.err.println("Such port doesn't exist");
                    }
                    else
                    {
                        // 串口不为空,读取数据
                        data = SerialPortManager.readFromPort(port);
                        for (int i = 0; i < data.length; i++)
                        {
                            // <CR><LF> carriage return（回车）在windows系统里值分别为13 10
                            if (data[i] == 13 && data[i + 1] == 10)
                            {
                                System.out.println("Send command \"" + currentCMD + "\" to the queue");
                                queue.add(currentCMD);
                                currentCMD = "";
                                i++;
                            }
                            else
                                currentCMD += (char)data[i];
                        }
                        System.out.println("currentCMD: " + currentCMD);
                    }
                }
                catch (Exception e)
                {
                    SerialPortManager.closePort(port);
                }
            }
        });
    }
}
