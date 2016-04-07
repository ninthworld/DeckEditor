package org.ninthworld.deckeditor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by NinthWorld on 4/5/2016.
 */
public class SetData {

    private String name, code, type, block;
    private int year, month, day;

    public SetData(){
        this.name = "";
        this.code = "";
        this.type = "";
        this.block = "";
        this.year = this.month = this.day = 0;
    }

    public void setDate(String date){
        String[] split = date.split("-");
        if(split.length > 2){
            this.year = Integer.parseInt(split[0]);
            this.month = Integer.parseInt(split[1]);
            this.day = Integer.parseInt(split[2]);
        }
    }

    public boolean isDateOlder(SetData set){
        if(this.year < set.getYear()){
            return true;
        }else if(this.year == set.getYear()){
            if(this.month < set.getMonth()){
                return true;
            }else if(this.month == set.getMonth()){
                if(this.day < set.getDay()){
                    return true;
                }
            }
        }

        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public static ArrayList<SetData> loadAllSets(InputStream file, ArrayList<SetData> sets, ArrayList<CardData> cards) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject allSets = (JSONObject) parser.parse(new InputStreamReader(file));

        for(Object setCodeObj : allSets.keySet()){
            SetData setData = new SetData();
            sets.add(setData);

            setData.setCode(setCodeObj.toString());
            JSONObject set = (JSONObject) allSets.get(setCodeObj);
            if(set.containsKey("name")){
                setData.setName(set.get("name").toString());
            }

            if(set.containsKey("releaseDate")){
                setData.setDate(set.get("releaseDate").toString());
            }

            if(set.containsKey("type")){
                setData.setType(set.get("type").toString());
            }

            if(set.containsKey("block")){
                setData.setBlock(set.get("block").toString());
            }

            if(set.containsKey("cards")){
                JSONArray cardsArray = (JSONArray) set.get("cards");
                cardsArray.forEach(card->{
                    JSONObject cardObj = (JSONObject) card;
                    CardData cardData = new CardData();
                    cards.add(cardData);
                    cardData.setSetCode(setData.getCode());

                    if(cardObj.containsKey("name")){
                        cardData.setName(cardObj.get("name").toString());
                    }

                    if(cardObj.containsKey("manaCost")){
                        cardData.setManaCost(cardObj.get("manaCost").toString());
                    }

                    if(cardObj.containsKey("cmc")){
                        cardData.setCmc((int) Double.parseDouble(cardObj.get("cmc").toString()));
                    }

                    if(cardObj.containsKey("type")){
                        cardData.setType(cardObj.get("type").toString());
                    }

                    if(cardObj.containsKey("rarity")){
                        cardData.setRarity(cardObj.get("rarity").toString());
                    }

                    if(cardObj.containsKey("text")){
                        cardData.setText(cardObj.get("text").toString());
                    }

                    if(cardObj.containsKey("names")){
                        JSONArray array = (JSONArray) cardObj.get("names");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setNames(strs);
                    }

                    if(cardObj.containsKey("colors")){
                        JSONArray array = (JSONArray) cardObj.get("colors");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setColors(strs);
                    }

                    if(cardObj.containsKey("colorIdentity")){
                        JSONArray array = (JSONArray) cardObj.get("colorIdentity");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setColorIdentity(strs);
                    }

                    if(cardObj.containsKey("supertypes")){
                        JSONArray array = (JSONArray) cardObj.get("supertypes");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setSupertypes(strs);
                    }

                    if(cardObj.containsKey("types")){
                        JSONArray array = (JSONArray) cardObj.get("types");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setTypes(strs);
                    }

                    if(cardObj.containsKey("subtypes")){
                        JSONArray array = (JSONArray) cardObj.get("subtypes");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setSubtypes(strs);
                    }

                    if(cardObj.containsKey("variations")){
                        JSONArray array = (JSONArray) cardObj.get("variations");
                        String[] strs = new String[array.size()];
                        for(int i=0; i<strs.length; i++){
                            strs[i] = array.get(i).toString();
                        }
                        cardData.setVariations(strs);
                    }

                    if(cardObj.containsKey("multiverseid")){
                        cardData.setMultiverseId(cardObj.get("multiverseid").toString());
                    }
                });
            }
        }

        return sets;
    }
}
