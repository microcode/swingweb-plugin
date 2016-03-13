package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("registration")
public class Registration {
    public String url;

    @XStreamAsAttribute
    public String status;

    @XStreamAsAttribute
    public String showing;

    @XStreamAsAttribute
    public String open;

    public String statusName;
    public String statusText;
}
