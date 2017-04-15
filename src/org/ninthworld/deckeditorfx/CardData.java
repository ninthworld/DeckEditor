package org.ninthworld.deckeditorfx;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by NinthWorld on 4/5/2016.
 */
public class CardData {

    private String setCode, name, manaCost, type, rarity, text, power, toughness, multiverseId;
    private String[] names, colors, colorIdentity, supertypes, types, subtypes, variations;
    private int cmc;

    public CardData(){
        this.setCode = "";
        this.name = "";
        this.manaCost = "";
        this.type = "";
        this.rarity = "";
        this.text = "";
        this.power = "";
        this.toughness = "";
        this.multiverseId = "";
        this.names = null;
        this.colors = null;
        this.colorIdentity = null;
        this.supertypes = null;
        this.types = null;
        this.subtypes = null;
        this.variations = null;
        this.cmc = 0;
    }

    public boolean hasVariations(){
        return (this.variations != null && this.variations.length > 0);
    }

    public int getVariationNum(){
        ArrayList<Integer> varis = new ArrayList<>();
        for(String s : this.variations){
            varis.add(Integer.parseInt(s));
        }
        varis.add(Integer.parseInt(this.multiverseId));
        Collections.sort(varis);
        return varis.indexOf(Integer.parseInt(this.multiverseId)) + 1;
    }

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public String getNameUnmodified(){
        return name;
    }

    public String getName() {
        if(this.getNames() != null && this.getNames().length > 1){
            String text = "";
            for(int i=0; i<this.getNames().length; i++){
                text += this.getNames()[i];
                if(i < this.getNames().length-1){
                    text += " // ";
                }
            }
            return text;
        }else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManaCost() {
        return manaCost;
    }

    public void setManaCost(String manaCost) {
        this.manaCost = manaCost;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getToughness() {
        return toughness;
    }

    public void setToughness(String toughness) {
        this.toughness = toughness;
    }

    public String[] getSubtypes() {
        return subtypes;
    }

    public void setSubtypes(String[] subtypes) {
        this.subtypes = subtypes;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    public String[] getSupertypes() {
        return supertypes;
    }

    public void setSupertypes(String[] supertypes) {
        this.supertypes = supertypes;
    }

    public String[] getColorIdentity() {
        return colorIdentity;
    }

    public void setColorIdentity(String[] colorIdentity) {
        this.colorIdentity = colorIdentity;
    }

    public String[] getColors() {
        return colors;
    }

    public void setColors(String[] colors) {
        this.colors = colors;
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public int getCmc() {
        return cmc;
    }

    public void setCmc(int cmc) {
        this.cmc = cmc;
    }

    public String getMultiverseId() {
        return multiverseId;
    }

    public void setMultiverseId(String multiverseId) {
        this.multiverseId = multiverseId;
    }

    public String[] getVariations() {
        return variations;
    }

    public void setVariations(String[] variations) {
        this.variations = variations;
    }
}
