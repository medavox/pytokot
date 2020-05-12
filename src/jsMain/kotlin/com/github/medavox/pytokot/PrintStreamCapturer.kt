package com.github.medavox.pytokot
/*
import org.w3c.dom.HTMLTextAreaElement

class PrintStreamCapturer : PrintStream {

    private val text:HTMLTextAreaElement
    private val atLineStart:Boolean
    private val indent:String

    PrintStreamCapturer(textArea:TextArea, capturedStream:PrintStream, String indent) {
        super(capturedStream);
        this.text = textArea;
        this.indent = indent;
        this.atLineStart = true;
    }

    PrintStreamCapturer(TextArea textArea, PrintStream capturedStream) {
        this(textArea, capturedStream, "");
    }

    private fun writeToTextArea(str:String) {
        if (text != null) {
            synchronized (text) {
                //text.setPositionCaret(text.getDocument().getLength());
                text.appendText(str);
            }
        }
    }

    private fun write(String str) {
        String[] s = str.split("\n", -1);
        if (s.length == 0)
            return;
        for (int i = 0; i < s.length - 1; i++) {
        writeWithPotentialIndent(s[i]);
        writeWithPotentialIndent("\n");
        atLineStart = true;
    }
        String last = s[s.length - 1];
        if (!last.equals("")) {
            writeWithPotentialIndent(last);
        }
    }

    private fun writeWithPotentialIndent(s:String) {
        if (atLineStart) {
            writeToTextArea(indent + s);
            atLineStart = false;
        } else {
            writeToTextArea(s);
        }
    }

    private fun newLine() {
        write("\n");
    }

    override fun print(b:Boolean) {
        synchronized (this) {
            super.print(b);
            write(String.valueOf(b));
        }
    }

    override fun print(c:Char) {
        synchronized (this) {
            super.print(c);
            write(String.valueOf(c));
        }
    }

    override fun print(s:CharArray) {
        synchronized (this) {
            super.print(s);
            write(String.valueOf(s));
        }
    }

    override fun print(d:Double) {
        synchronized (this) {
            super.print(d);
            write(String.valueOf(d));
        }
    }

    override fun print(f:Float) {
        synchronized (this) {
            super.print(f);
            write(String.valueOf(f));
        }
    }

    override fun print(i:Int) {
        synchronized (this) {
            super.print(i);
            write(String.valueOf(i));
        }
    }

    override fun print(l:Long) {
        synchronized (this) {
            super.print(l);
            write(String.valueOf(l));
        }
    }

    override fun print(Object o) {
        synchronized (this) {
            super.print(o);
            write(String.valueOf(o));
        }
    }

    override fun print(s:String) {
        synchronized (this) {
            super.print(s);
            if (s == null) {
                write("null");
            } else {
                write(s);
            }
        }
    }

    override fun println() {
        synchronized (this) {
            newLine();
            super.println();
        }
    }

    override fun println(b:Boolean) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(c:Char) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(i:Int) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(l:Long) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(f:Float) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(d:Double) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(char x[]) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(x:String) {
        synchronized (this) {
            print(x);
            newLine();
            super.println();
        }
    }

    override fun println(x:Object) {
        String s = String.valueOf(x);
        synchronized (this) {
            print(s);
            newLine();
            super.println();
        }
    }
}
*/
