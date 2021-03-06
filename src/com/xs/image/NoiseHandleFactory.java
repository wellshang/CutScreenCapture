package com.xs.image;



public class NoiseHandleFactory implements BaseNoiseHandleFactory {
    private static NoiseHandleFactory noiseHandlerFactory;
    private NoiseHandleFactory(){
    }
    
    public static BaseNoiseHandleFactory getBaseNoiseHandlerFactory() {
        // TODO Auto-generated method stub
        if(noiseHandlerFactory == null){
            noiseHandlerFactory = new NoiseHandleFactory();
        }
        return noiseHandlerFactory;
    }

    public BaseNoiseHandle getNoiseHandle(String noiseType) {
        // TODO Auto-generated method stub
        try {
            Class<?> clazz;
            if(!noiseType.equals("undefined")){
                clazz = Class.forName("com.xs.image."+noiseType+"Handle");
            }else{
                clazz = Class.forName("com.xs.image.NoiseHandle");
            }
            return (BaseNoiseHandle) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return null; 
    }

}
