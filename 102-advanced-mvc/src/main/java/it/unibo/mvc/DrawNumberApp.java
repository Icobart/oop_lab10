package it.unibo.mvc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import it.unibo.mvc.Configuration.Builder;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    
    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param config the path of the configuration file
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String config, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        
        final Builder setup = new Builder();
        try(var readVar = new BufferedReader(new InputStreamReader(
            ClassLoader.getSystemResourceAsStream(config)))){
            for(var saveString = readVar.readLine(); saveString!=null; saveString=readVar.readLine()) {
                final StringTokenizer lineRead = new StringTokenizer(saveString);
                final List<String> line = new ArrayList<>();
                while(lineRead.hasMoreElements()) {
                    line.add(lineRead.nextToken());
                }
                if(line.size() == 2) {
                    if(line.contains("minimum:")) {
                        setup.setMin(Integer.parseInt(line.get(1)));
                    }
                    else if(line.contains("maximum:")) {
                        setup.setMax(Integer.parseInt(line.get(1)));
                    }
                    else if(line.contains("attempts:")) {
                        setup.setAttempts(Integer.parseInt(line.get(1)));
                    }
                } else {
                    displayError(saveString + " not enough information");
                }
            }
        } catch(IOException | NumberFormatException e) {
            displayError(e.getMessage());
        }
        final Configuration builder = setup.build();
        if(builder.isConsistent()) {
            this.model = new DrawNumberImpl(builder);
        } else {
            displayError("inconsistent configuration: minimum: "
            + builder.getMin() + " maximum: " + builder.getMax()
            + " attempts: " + builder.getAttempts());
            this.model = new DrawNumberImpl(new Builder().build());
        }
    }

    private void displayError(final String message) {
        for(final DrawNumberView view : views) {
            view.displayError(message);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl());
    }

}
