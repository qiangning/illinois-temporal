package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelationType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by chuchu on 12/19/17.
 */
public class TemporalRelType extends BinaryRelationType {
    public enum relTypes{
        BEFORE("BEFORE"),
        AFTER("AFTER"),
        EQUAL("EQUAL"),
        VAGUE("VAGUE"),
        NULL("NULL");
        private final String name;
        private relTypes inverse;
        relTypes(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }
        public relTypes getInverse(){
            if(inverse==null)
                inverse = this.inverse();
            return inverse;
        }
        private relTypes inverse(){
            switch(this){
                case BEFORE:
                    return AFTER;
                case AFTER:
                    return BEFORE;
                case EQUAL:
                    return EQUAL;
                case VAGUE:
                    return VAGUE;
                default:
                    return NULL;
            }
        }
    }

    private relTypes reltype;
    private static TemporalRelType nullRelType;

    /*Constructors*/

    public TemporalRelType(relTypes reltype) {
        this.reltype = reltype;
    }

    public TemporalRelType(String reltype) {
        for(relTypes rel:relTypes.values()){
            if(rel.getName().equals(reltype)||rel.getName().toLowerCase().equals(reltype)) {
                this.reltype = rel;
                return;
            }
        }
        this.reltype = relTypes.VAGUE;
        System.out.printf("Error using TemporalRelType (%s): %s cannot be found.\n",reltype,reltype);
    }

    /*Functions*/
    public TemporalRelType inverse() {
        return new TemporalRelType(reltype.getInverse());
    }

    public String toString() {
        return getReltype().getName();
    }

    public void reverse() {
        reltype = reltype.getInverse();
    }

    public boolean isNull() {
        return reltype==null || reltype==relTypes.NULL;
        //return reltype==null;
    }

    public boolean isEqual(BinaryRelationType other) {
        return this.reltype.equals(((TemporalRelType) other).reltype);
    }

    @Override
    public List<BinaryRelationType> transitivity(BinaryRelationType rel1, BinaryRelationType rel2) {
        switch(((TemporalRelType) rel1).getReltype()){
            case BEFORE:
                switch (((TemporalRelType)rel2).getReltype()){
                    case BEFORE:
                    case EQUAL:
                        return Arrays.asList(new BinaryRelationType[]{new TemporalRelType(relTypes.BEFORE)});
                    default:
                        return Arrays.asList(new BinaryRelationType[]{new TemporalRelType(relTypes.BEFORE),
                                new TemporalRelType(relTypes.AFTER),
                                new TemporalRelType(relTypes.EQUAL),
                                new TemporalRelType(relTypes.VAGUE)});
                }
            case AFTER:
                switch (((TemporalRelType)rel2).getReltype()){
                    case AFTER:
                    case EQUAL:
                        return Arrays.asList(new BinaryRelationType[]{new TemporalRelType(relTypes.AFTER)});
                    default:
                        return Arrays.asList(new BinaryRelationType[]{new TemporalRelType(relTypes.BEFORE),
                                new TemporalRelType(relTypes.AFTER),
                                new TemporalRelType(relTypes.EQUAL),
                                new TemporalRelType(relTypes.VAGUE)});
                }
            case EQUAL:
                return Arrays.asList(new BinaryRelationType[]{new TemporalRelType(((TemporalRelType)rel2).getReltype())});
            default:
                return Arrays.asList(new BinaryRelationType[]{new TemporalRelType(relTypes.BEFORE),
                        new TemporalRelType(relTypes.AFTER),
                        new TemporalRelType(relTypes.EQUAL),
                        new TemporalRelType(relTypes.VAGUE)});
        }
    }

    /*Getters and Setters*/

    public relTypes getReltype() {
        return reltype;
    }

    public void setReltype(relTypes reltype) {
        this.reltype = reltype;
    }

    public static TemporalRelType getNullTempRel() {
        if(nullRelType==null){
            nullRelType = new TemporalRelType(relTypes.NULL);
        }
        return nullRelType;
    }

    public static void main(String[] args) throws Exception{
        TemporalRelType tmp = new TemporalRelType("BEFORE");
        tmp.reverse();
        System.out.println(tmp);
        TemporalRelType tmp2 = new TemporalRelType("BEFORE");
        tmp2.inverse();
        System.out.println(tmp2);
    }
}
