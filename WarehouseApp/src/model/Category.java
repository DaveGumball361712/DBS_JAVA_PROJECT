package model;

public class Category {
    private int categoryId;
    private Integer parentCategoryId;
    private String name;
    private String description;

    public Category(){
    }

    public Category(int categoryId, Integer parentCategoryId, String name, String description){
        this.categoryId = categoryId;
        this.parentCategoryId = parentCategoryId;
        this.name = name;
        this.description = description;
    }

    public int getCategoryId(){
        return categoryId;
    }

    public void setCategoryId(int categoryId){
        this.categoryId = categoryId;
    }

    public Integer getParentCategoryId(){
        return parentCategoryId;
    }

    public void setParentCategoryId(Integer parentCategoryId){
        this.parentCategoryId = parentCategoryId;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getDescription(){
        return description;
    }

    public void setDesciption(String description){
        this.description = description;
    }

    @Override 
    public String toString(){
        return name;
    }
}
