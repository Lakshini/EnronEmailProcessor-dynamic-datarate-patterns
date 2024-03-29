/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.common.benchmarks.http;

import com.google.common.base.Splitter;
import com.uebercomputing.mailrecord.MailRecord;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.joda.time.format.ISODateTimeFormat;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.apache.kafka.clients.producer.Producer;
import net.minidev.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;

import java.util.*;

import java.util.concurrent.locks.ReentrantLock;
import java.io.*;


/**
 * Data loader for EDGAR log files.
 */

public class EnronDataLoader extends Thread {
    private long events = 0;
    private long startTime;

    long dataStartTime;
    int count;
    public static BufferedWriter bw;

    private Splitter splitter = Splitter.on(',');
    private InputHandler inputHandler;
    long startTime2;
    private int temp = 0;
    private long byteCount = 0L;
    EnronDataLoader enronDataLoader = null;
    static final ReentrantLock lock = new ReentrantLock();

    private static final Logger log = Logger.getLogger(EnronDataLoader.class);

    public static void main(String[] args) {

        BasicConfigurator.configure();

        log.info("Welcome to kafka message sender");

        try {
            File file = new File("/home/madushi/FYP/FYP_New/Email_processor/datarate.csv");

            BufferedWriter bw = null;
            FileWriter fw = null;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write("Average Time stamp, " +
                    "Data Rate");

            bw.write("\n");
            bw.flush();
            bw.close();

        } catch (Exception e) {
            System.out.println("Error when writing to the file");
        }

        EnronDataLoader loader1 = new EnronDataLoader(bw);

        loader1.start();
    }

    public EnronDataLoader(BufferedWriter bw) {
        enronDataLoader = this;
        this.bw = bw;
    }

    public EnronDataLoader(EnronDataLoader js) {
        enronDataLoader = js;
    }

    public void incrementCommon(BufferedWriter bw,String jsonMessage) {
        enronDataLoader.temp++;
        try {
            enronDataLoader.byteCount+=jsonMessage.getBytes("UTF-8").length;
            log.info("Byte Count +++++++++++++++++++++++++++++++++++++"+byteCount);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (enronDataLoader.temp == 1) {
            enronDataLoader.startTime2 = System.currentTimeMillis();
            enronDataLoader.dataStartTime = enronDataLoader.startTime2;
            enronDataLoader.count =0;
        }
        long diff = System.currentTimeMillis() - enronDataLoader.startTime2;
        if (diff != 0){
            log.info(Thread.currentThread().getName() + " spent : "
                    + diff + " for the event count : " + enronDataLoader.temp
                    + " with the  Data rate : " + (enronDataLoader.byteCount * 1000  / diff)+ " bytes per second and Time taken "+diff);
            try {
                if((System.currentTimeMillis()-dataStartTime)> 10000){

                    bw.write(enronDataLoader.count + "," + (enronDataLoader.byteCount * 1000 / diff) + "\n");
                    bw.flush();
                    enronDataLoader.dataStartTime = System.currentTimeMillis();
                    enronDataLoader.count+=1;
                }

            } catch(Exception e){
                e.printStackTrace();
                System.out.println("Exception occured while writing to the file");
            }
        }

        log.info(Thread.currentThread().getName() + " spent : "
                + diff + " for the event count : " + enronDataLoader.temp
                + " with the  Data rate : " + (enronDataLoader.temp * 1000  / diff));
    }

    public EnronDataLoader(InputHandler inputHandler) {
        super("Data Loader");
        this.inputHandler = inputHandler;
    }



    public void run() {
        BufferedReader br = null;

        BufferedWriter bw = null;
        try{

            File file = new File("/home/madushi/FYP/FYP_New/Email_processor/datarate.csv");
            FileWriter fw = null;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);} catch (Exception e){
            System.out.println("Error reading");
        }

        ArrayList<Integer> list = new ArrayList<Integer>();

        list.add(0);
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        list.add(7);
        list.add(8);
        list.add(9);
        list.add(13);
        list.add(14);

        try {
            Producer<String, String> producer = KafkaMessageSender.createProducer();
            Locale locale = new Locale("en", "US");
            ResourceBundle bundle2 = ResourceBundle.getBundle("config", locale);

            String inputFilePath = bundle2.getString("input");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath),
                    Charset.forName("UTF-8")));

            DatumReader<MailRecord> userDatumReader = new SpecificDatumReader<MailRecord>(MailRecord.class);
            DataFileReader<MailRecord> dataFileReader = new DataFileReader<MailRecord>(new File(inputFilePath), userDatumReader);
            int grouID = 1;

            int i = 0;
            int partitionCount =1;

            while (dataFileReader.hasNext() && dataFileReader!= null) {
                JSONObject jsonObject = new JSONObject();

                String toAddresses = "";
                String ccAddresses = "";
                String bccAddresses="";
                String subject ="";
                String body = "";
                String fromAddress = "";

                MailRecord email = dataFileReader.next();

                jsonObject.put("iij_timestamp",System.currentTimeMillis());
                jsonObject.put("groupID",grouID);

                lock.lock();
                if(grouID==3){
                    grouID=1;
                }
                grouID++;
                lock.unlock();

                fromAddress = new String(email.getFrom().toString().getBytes("ISO-8859-1"), "UTF-8");
                jsonObject.put("fromAddress",fromAddress);

                Iterator<CharSequence> itr = null;
                StringBuilder sb = new StringBuilder();

                final List<CharSequence> to = email.getTo();
                if(to != null) {
                    itr = to.iterator();

                    while (itr.hasNext()) {
                        sb.append(itr.next());
                        if(itr.hasNext()){
                            sb.append(",");
                        }
                    }
                }
                toAddresses = new String(sb.toString().getBytes("ISO-8859-1"),"UTF-8");
                jsonObject.put("toAddresses",toAddresses);

                sb = new StringBuilder();

                final List<CharSequence> cc = email.getCc();
                if(cc != null) {
                    itr = cc.iterator();

                    while (itr.hasNext()) {
                        sb.append(itr.next());
                        if(itr.hasNext()){
                            sb.append(",");
                        }
                    }
                }

                ccAddresses = new String(sb.toString().getBytes("ISO-8859-1"),"UTF-8");
                jsonObject.put("ccAddresses",ccAddresses);

                sb = new StringBuilder();
                final List<CharSequence> bcc = email.getBcc();
                if(bcc != null) {
                    itr = bcc.iterator();

                    while (itr.hasNext()) {
                        sb.append(itr.next());
                        if(itr.hasNext()){
                            sb.append(",");
                        }
                    }
                }

                bccAddresses = new String(sb.toString().getBytes("ISO-8859-1"),"UTF-8");
                jsonObject.put("bccAddresses", bccAddresses);

                subject = new String(email.getSubject().toString().getBytes("ISO-8859-1"),"UTF-8");
                jsonObject.put("subject",subject);

                body = new String(email.getBody().toString().getBytes("ISO-8859-1"),"UTF-8");
                jsonObject.put("body",body);

                jsonObject.put("regexstr","(.*)@enron.com");


                String jsonMEssage = "{\"event\":"+jsonObject.toJSONString()+"}";
//                System.out.println("print -------------------- "+jsonMEssage);

                try {
                    KafkaMessageSender.runProducer(jsonMEssage.toString(),producer);
                    log.info("Message sent to kafaka by "
                            + Thread.currentThread().getName());

                    incrementCommon(bw,jsonMEssage.toString());

                    try {
                        Thread.currentThread().sleep(1); //increase upto 500
                    } catch (InterruptedException e) {
                        log.info("Error: " + e.getMessage());
                    }

                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        log.info("Error: " + e.getMessage());
                    }

                } catch (InterruptedException e) {
                    log.error("Error sending an event to Input Handler, " + e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Error: " + e.getMessage(), e);
                }

            }
        } catch (FileNotFoundException e) {
            log.error("Error in accessing the input file. " + e.getMessage(), e);
        } catch (IOException e2) {
            log.error("Error in accessing the input file. " + e2.getMessage(), e2);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("Error in accessing the input file. " + e.getMessage(), e);
                }
            }
        }
    }
}
