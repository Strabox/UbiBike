package pt.ulisboa.tecnico.cmu.ubibike.domain;

import java.io.Serializable;

/**
 * Created by André on 21-03-2016.
 */
public class Bike implements Serializable{

    private String identifier;

    private Coordinates position;

    public Bike(){ }

    public void setIdentifier(String id){
        this.identifier = id;
    }

    public String getIdentifier(){
        return this.identifier;
    }

    public Coordinates getPosition(){
        return this.position;
    }

    public void setPosition(Coordinates position){
        this.position = position;
    }

}
