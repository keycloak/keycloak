package com.dell.software.ce.dib.claims;

public class ClaimMapping<K> {
    private K match;
    private K replacement;
    private boolean isRegex = false;

    public ClaimMapping() {

    }

    public ClaimMapping(K match, K replacement, boolean isRegex) {
        this.match = match;
        this.replacement = replacement;
        this.isRegex = isRegex;
    }

    public K getMatch() {
        return match;
    }

    public K getReplacement() {
        return replacement;
    }

    public boolean isRegex() {
        return isRegex;
    }

    public boolean getRegex() {
        return isRegex;
    }

    public void setMatch(K match) {
        this.match = match;
    }

    public void setReplacement(K replacement) {
        this.replacement = replacement;
    }

    public void setRegex(boolean isRegex) {
        this.isRegex = isRegex;
    }
}
