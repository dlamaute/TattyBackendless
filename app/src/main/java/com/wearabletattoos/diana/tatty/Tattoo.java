package com.wearabletattoos.diana.tatty;

import android.graphics.Bitmap;

/**
 * Created by Diana on 1/5/16.
 */
public class Tattoo {
    private String objectID;
    private String uid; //unique id of the rfid tag scanned
    private String owner; //username of the owner, not a full user instance
    private String name; //name assigned to tattoo; modifiable by owner
    private String message; //message assigned to tattoo; modifiable by owner
    private Bitmap image; //image assigned to tattoo; modifiable by owner

    public Tattoo() {}

    public String getObjectID() {
        return objectID;
    }
    public void setObjectID(String uid1) {
        this.objectID = uid1;
    }

    public String getUID() {
        return uid;
    }
    public void setUID(String uid1) {
        this.uid = uid1;
    }

    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner1) {
        this.owner = owner1;
    }

    public String getName() {
        return name;
    }
    public void setName(String name1) {
        this.name = name1;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message1) {
        this.message = message1;
    }

    public Bitmap getImage() {
        return image;
    }
    public void setImage(Bitmap image1) {
        this.image = image1;
    }

    public String toString() {
        return "Tattoo " + this.uid + " with owner " + this.owner;
    }
}
