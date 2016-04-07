package grid;
/*
 * This file is part of JGAP. JGAP offers a dual license model containing the
 * LGPL as well as the MPL. For licensing information please see the file
 * license.txt included with JGAP or have a look at the top of class
 * org.jgap.Chromosome which representatively includes the JGAP license policy
 * applicable for any file delivered with JGAP.
 */

import org.apache.log4j.Logger;
import org.jgap.distr.grid.IClientFeedback;
import org.jgap.distr.grid.JGAPRequest;
import org.jgap.distr.grid.JGAPResult;

/**
 * Listener for feedback sent to the client. This is a simple sample
 * implementation.
 *
 * @author Klaus Meffert
 * @since 3.1
 */
public class MyClientFeedback implements IClientFeedback {

    private final static String className = MyClientFeedback.class.getName();

    private static Logger log = Logger.getLogger(className);

    public MyClientFeedback() {
    }

    @Override
    public void error(String msg, Exception ex) {
        log.error("Error catched on client side: " + msg, ex);
    }

    @Override
    public void sendingFragmentRequest(JGAPRequest req) {
        log.warn("Sending work request " + req.getRID());
    }

    @Override
    public void receivedFragmentResult(JGAPRequest req, JGAPResult res, int idx) {
        log.warn("Receiving work (index " + idx + "). First solution: " + res.getPopulation().getChromosome(0));
    }

    @Override
    public void beginWork() {
        log.warn("Client starts sending work requests");
    }

    @Override
    public void endWork() {
        log.warn("Your request was processed completely");
    }

    @Override
    public void info(String a_msg) {
        log.warn(a_msg);
    }

    @Override
    public void setProgressMaximum(int max) {
    }

    @Override
    public void setProgressMinimum(int min) {
    }

    @Override
    public void setProgressValue(int val) {
    }

    public void setRenderingTime(MyRequest req, long dt) {
    }

    @Override
    public void completeFrame(int idx) {
        log.warn("Client notified that unit " + idx + " is finished.");
    }
}