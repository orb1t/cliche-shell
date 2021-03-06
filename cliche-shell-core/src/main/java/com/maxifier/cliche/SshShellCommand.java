package com.maxifier.cliche;

import com.maxifier.cliche.util.EmptyMultiMap;

import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.PersistentHistory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author aleksey.didik@maxifier.com Aleksey Didik
 */
class SshShellCommand implements Command {

    private final ExecutorService executor;
    private final String promt;
    private final String appName;
    private final Collection<?> handlersCollection;
    private final int port;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    private ExitCallback callback;
    private Future future;
    private ConsoleReader consoleReader;
    private Shell theShell;

    public SshShellCommand(ExecutorService executor,
                           String promt,
                           String appName,
                           Collection<?> handlersCollection,
                           int port) {
        this.executor = executor;
        this.promt = promt;
        this.appName = appName;
        this.handlersCollection = handlersCollection;
        this.port = port;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = new PrintStream(new TtyFilterOutputStream(out));
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = new PrintStream(err);
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        this.future = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    consoleReader = new ConsoleReader(in, out);
                    consoleReader.setBellEnabled(true);
                    //history file is port based to separate apps
                    File historyFile = new File(System.getProperty("user.home"), ".clhistory" + port);
                    consoleReader.setHistory(new FileHistory(historyFile));
                    consoleReader.setHistoryEnabled(true);
                    consoleReader.setExpandEvents(false);

                    ConsoleIO io = new ConsoleIO(consoleReader, out, out);
                    List<String> path = new ArrayList<String>(1);
                    path.add(promt);
                    theShell = new Shell(new Shell.Settings(io, io, new EmptyMultiMap(), false),
                            new CommandTable(new DashJoinedNamer(true)), path);
                    theShell.setAppName(appName);
                    theShell.addMainHandler(theShell, "!");
                    theShell.addMainHandler(new HelpCommandHandler(), "?");
                    for (Object h : handlersCollection) {
                        theShell.addMainHandler(h, "");
                    }
                    consoleReader.addCompleter(
                            new AggregateCompleter(
                                    new StringsCompleter(theShell.getCommandTable().getAbbreviates("")),
                                    new StringsCompleter(theShell.getCommandTable().getCommandsNames(""))));
                    theShell.commandLoop();
                    out.println("Bye!");
                    out.flush();
                    callback.onExit(0);
                } catch (Exception e) {
                    callback.onExit(1);
                } finally {
                    consoleReader.shutdown();
                    flushHistory();
                }
            }
        });
    }

    void flushHistory() {
        History history = consoleReader.getHistory();
        if (history instanceof PersistentHistory) {
            try {
                ((PersistentHistory) history).flush();
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    public void destroy() {
        this.future.cancel(false);
    }


    protected class TtyFilterOutputStream extends FilterOutputStream {
        public TtyFilterOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int c) throws IOException {
            if (c == '\n') {
                super.write('\r');
            }/* else if (c == '\r') {
                c = '\n';
            }*/
            super.write(c);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = off; i < len; i++) {
                write(b[i]);
            }
        }
    }

}
