package com.secure.scope;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CellAndScope {
    private List<BigInteger> cell;
    private List<ArrayList<Double>> Scope;

    public CellAndScope() {
    }

    public CellAndScope(List<BigInteger> cell, List<ArrayList<Double>> scope) {
        this.cell = cell;
        Scope = scope;
    }

    public List<BigInteger> getCell() {
        return cell;
    }

    public void setCell(List<BigInteger> cell) {
        this.cell = cell;
    }

    public List<ArrayList<Double>> getScope() {
        return Scope;
    }

    public void setScope(List<ArrayList<Double>> scope) {
        Scope = scope;
    }
}
