package yy.project.YOYO.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import yy.project.YOYO.domain.Food;
import yy.project.YOYO.domain.Team;
import yy.project.YOYO.domain.User;
import yy.project.YOYO.service.FoodService;
import yy.project.YOYO.service.TeamService;
import yy.project.YOYO.service.UserService;
import yy.project.YOYO.vo.FoodVO;
import yy.project.YOYO.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final FoodService foodService;

    private final UserService userService;

    private final TeamService teamService;


    @GetMapping("/admin/adminFood")
    public String adminFood(Model model){

        model.addAttribute("allFood",foodService.findAll());

        return "admin/adminFood";
    }

    @GetMapping("/admin/adminModify")
    public String adminModify(){
        return "admin/adminModify";
    }
    @GetMapping("/admin/adminAdd")
    public String adminAdd(){
        return "admin/adminAdd";
    }

    @GetMapping("/admin/adminUser")
    public String adminUser(Model model, @PageableDefault( page = 0, size = 10, sort = "uID", direction = Sort.Direction.DESC) Pageable pageable,
                            @RequestParam(required = false, defaultValue = "") String searchWord){

        Page<User> list = null;
        String searchWord1 = searchWord;
        String searchWord2 = searchWord;
        String searchWord3 = searchWord;
        String searchWord4 = searchWord;


        if(searchWord == null){
            list = userService.findAll(pageable);

        }else{
            list = userService.findByUserIDIgnoreCaseContainingOrAddressContainingOrUserNameIgnoreCaseContainingOrEmailIgnoreCaseContaining(searchWord1, searchWord2, searchWord3, searchWord4, pageable);
        }


        int nowPage = list.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage -5, 1);
        int endPage = Math.min(nowPage + 4, list.getTotalPages());


        model.addAttribute("Users", list);
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("searchWord", searchWord);
        model.addAttribute("totalPage", list.getTotalPages());



        return "admin/adminUser";
    }

    //?????? ??????
    @PostMapping("/admin/withdrawUser")
    public String withdrawUser(UserVO vo, Model model){

        //System.out.println(vo.getUserList().size());


        /*for(String user : vo.getUserList()){
            System.out.println(user + " ");
        }*/

        userService.deleteByUserIDIn(vo.getUserList());

        return "redirect:/admin/adminUser";
    }


    //??????
    @PostMapping("/admin/foodAdd")
    public ResponseEntity<String> foodAdd(FoodVO vo, HttpServletRequest request, Food food) {

        ResponseEntity<String> entity = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "html", Charset.forName("UTF-8")));

        int cnt = foodService.checkFood(vo.getFoodName());

        if(cnt>0){
            String msg = "<script>alert('?????? ?????? ???????????????.'); history.back();</script>";

            entity = new ResponseEntity<String>(msg, headers, HttpStatus.BAD_REQUEST);
        }
        else{
            //?????? ?????? ????????? ?????? ?????? ??????


            String path = new File("").getAbsolutePath()+File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"static" + File.separator+"adminImage" + File.separator+"food";



            try{
                MultipartHttpServletRequest mr = (MultipartHttpServletRequest)  request;

                MultipartFile file = mr.getFile("filename");

                String orgFileName = file.getOriginalFilename();

                if(orgFileName != null && !orgFileName.equals("")){

                    //System.out.println("origin" + orgFileName);

                    File f = new File(path, orgFileName);

                    //?????? ?????? ????????? ????????? ?????? ?????? ?????? ??????
                    if(f.exists()){

                        for(int num=1; ; num++){

                            int idx = orgFileName.lastIndexOf(".");

                            String fileName = orgFileName.substring(0, idx);

                            String ext = orgFileName.substring(idx+1);

                            f = new File(path, fileName + "(" + num + ")." + ext);

                            if(!f.exists()){
                                orgFileName = f.getName();
                                break;
                            }
                        }//for ?????? ??????

                    }//?????? ?????? ?????? ??????


                    try{
                        //?????? ?????????
                        file.transferTo(f);
                        System.out.println("?????? ?????????");
                        vo.setFoodImg(orgFileName);

                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }

                System.out.println(">>"+ vo.getFoodImg());

                food.setFoodImg(vo.getFoodImg());

                if(vo.getEvent().equals("")){
                    food.setEvent(null);
                }

                //?????? ??????
                foodService.save(food);

                String msg = "<script>alert('????????? ????????? ?????????????????????.');location.href='/admin/adminFood'; </script>";

                entity = new ResponseEntity<String>(msg, headers,HttpStatus.OK );


            }catch (Exception e){
                e.printStackTrace();
                //?????? ??????
                deleteFile(path, vo.getFoodImg());

                String msg = "<script>alert('????????? ????????? ?????????????????????.'); history.back(); </script>";
                entity = new ResponseEntity<String>(msg, headers,HttpStatus.BAD_REQUEST );
            }

        }
            return entity;

    }


    //?????? ?????? ??????
    @PostMapping("/showFoods")
    @ResponseBody
    public List<Food> showFoods(@RequestParam("foodType") String type) {

        List<Food> food = foodList(type);

        System.out.println("hello");

        return food;
    }


    //??????
    @PostMapping("/admin/getFoodData")
    @ResponseBody
    public Food searchFoodData(@RequestParam("searchFood") String searchFood){
        return foodService.findByFoodName(searchFood);
    }

    @PostMapping("/selectFoodOk")
    @ResponseBody
    public String selectFoodOk(@RequestParam("fID") Long fID, @RequestParam("tID") Long tID){

        Team team = teamService.findBytID(tID);

        Food food = foodService.findByfID(fID);
        System.out.println(food.getFoodName());

        System.out.println(team.getTeamName());

        team.setFood(food);

        teamService.save(team);

        return "OK";
    }


    //??????
    @PostMapping("/admin/foodModify")
    public ResponseEntity<String> foodModify(FoodVO vo, HttpServletRequest request, Food food){

        ResponseEntity<String> entity = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "html", Charset.forName("UTF-8")));


        Food modifyFood = foodService.findByFoodName(food.getFoodName());

        //?????? ?????? ??????
        String beforeFile = foodService.findByFoodName(vo.getFoodName()).getFoodImg();

        String path = new File("").getAbsolutePath()+File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"static" + File.separator+"adminImage" + File.separator+"food";

        try{

            MultipartHttpServletRequest mr = (MultipartHttpServletRequest) request;

            MultipartFile newfile = mr.getFile("filename");

            if(newfile !=null){
                //????????? ?????? ????????? ?????? ??????

                String fileName = newfile.getOriginalFilename();
                System.out.println(">>>>new " + fileName);

                if(fileName != null && !fileName.equals("")){

                    System.out.println(">>>>here " + fileName);

                    File f = new File(path, fileName);

                    if(f.exists()){
                        //?????? ?????? ????????? ????????? ?????? ??????
                        for(int n=1; ; n++){
                            int idx = fileName.lastIndexOf(".");
                            String fileNameExt = fileName.substring(0, idx);
                            String ext = fileName.substring(idx+1);

                            f = new File(path, fileNameExt + "(" + n + ")." + ext);

                            if(!f.exists()){
                                fileName = f.getName();
                                break;
                            }
                        }//for
                    }// if

                    //?????? ??????
                    try{
                        newfile.transferTo(f);
                        System.out.println("?????? ?????????");

                        if(!newfile.isEmpty()){ //????????? ??????????????? ?????? ??????
                            deleteFile(path, beforeFile);
                        }

                        vo.setFoodImg(fileName);

                        food.setFoodImg(vo.getFoodImg());
                        System.out.println(food.getFoodImg());

                    }catch (Exception ex){

                    }
                }//if
            }//if

            if(vo.getEvent().equals("")){
                vo.setEvent(null);
            }

            modifyFood.setFoodCategory(vo.getFoodCategory());
            modifyFood.setEvent(vo.getEvent());
            modifyFood.setPriority(vo.getPriority());
            modifyFood.setSeason(vo.getSeason());
            modifyFood.setTemperature(vo.getTemperature());
            modifyFood.setWeather(vo.getWeather());

            if(vo.getFoodImg() != null && !vo.getFoodImg().equals("")){
                modifyFood.setFoodImg(vo.getFoodImg());
            }
            foodService.save(modifyFood);

            String msg = "<script>alert('?????? ????????? ?????????????????????.');location.href='/admin/adminFood';</script>";

            entity = new ResponseEntity<String>(msg, headers, HttpStatus.OK);

        }catch (Exception e){
            e.printStackTrace();

            deleteFile(path, vo.getFoodImg());

            String msg = "<script>alert('?????? ????????? ?????????????????????.');history.back();</script>";
            entity = new ResponseEntity<String>(msg, headers, HttpStatus.BAD_REQUEST);

        }
        return entity;
    }

    @RequestMapping(value="/getFoodRecommend", method=RequestMethod.GET)
    @ResponseBody
    public List<Food> foodRecommend(String weather, String temperature){

        System.out.println(weather);
        System.out.println(temperature);
        //?????? ??????
        //?????? ?????? ?????? ???????????? 2???

        //?????? ?????? ???????????? ?????? ?????? ?????????
        List<Food> list = new ArrayList<Food>();

        //?????? ??????, ?????? ??????, ?????? ?????? ?????? ?????? ????????? ?????????
        ///////////////////////////////////////////
        HashSet<Food> foods = new HashSet<Food>();


        int cnt=0;
        //cnt<=2?????? ??????
        //1. ?????? ????????? ???????????? ????????? ?????? ????????? ?????? -- 0???, 1???, 2??? ??????...(????????? ????????? ?????? 1??? ???????????? ?????? ???????????? ??????)
        //?????? ??????
        LocalDate now = LocalDate.now();
        System.out.println(now.toString());
        //?????? ????????? ???????????? ????????? ?????? ?????? ??????
        List<Food> event = foodService.findByEvent(now.toString());


        if(event.size()>=2) {
            //1??? ????????????
            Collections.shuffle(event);
            list.add(event.get(0));
            cnt=1;
        }else if(event.size()==1) {
            //1???
            list.add(event.get(0));
            cnt=1;
        }
        //0?????? ?????? - cnt = 0 ????????????


        //2. ?????? ????????? ???????????? ?????? ????????? ??????
        //?????? ??????
        //System.out.println(weather);

        String todayWeather = "0";

        if(weather.contains("??????")) {
            todayWeather = "clear";

        }else if(weather.contains("???") || weather.contains("?????????")) {

            todayWeather = "rain";

        }else if(weather.contains("???")) {
            todayWeather = "snow";

        }
        if(!todayWeather.equals("0")) {
            foods.addAll(foodService.findByWeather(todayWeather));
        }


        //3. ?????? ????????? ???????????? ?????? ????????? ??????
        int month = now.getMonthValue();

        String season = "0";

        if(month>=3 && month <=5) {
            season = "spring";
        }else if(month>=6 && month <=8) {
            season = "summer";
        }else if(month>=9 && month <=11) {
            season = "fall";
        }else if(month <= 2 || month ==12) {
            season = "winter";
        }

        if(!season.equals("0")) {
            foods.addAll(foodService.findBySeason(season));
        }


        //4. ?????? ????????? ???????????? ?????? ????????? ??????
        Double tem = Double.parseDouble(temperature);
        String temp = "0";

        if(tem <=15) {
            temp = "2";
        }else if(tem>=25) {
            temp= "1";
        }

        if(!temp.equals("0")) {
            foods.addAll(foodService.findByTemperature(temp));
        }



        //event?????? ????????? ?????? ???????????? ????????? ??????
        if(list.size()>0) {
            foods.removeIf(FoodVO->FoodVO.getFoodName().equals(list.get(0).getFoodName()));
        }

        // 2,3,4????????? ?????? ?????? ??????
        //fname?????? ?????? ??????

        for(Food fvo: foods) {
            System.out.println(fvo.getFoodName());
        }

        List<Food> f = new ArrayList<Food>(foods);

        //2,3,4, ?????? ???????????? ?????? <- (1?????? 0????????? 2??? ??????, 1??? ???????????? 1??? ??????)


        if(f.size()>0) {

            Collections.shuffle(f);
            list.add(f.get(0));
            cnt++;

            if(cnt==1 && f.size()>1) {
                list.add(f.get(1));
                cnt++;
            }
        }

        for(Food fvo: list) {
            System.out.println(fvo.getFoodName()+ "+");
            System.out.println(fvo.getFID() + " ");
        }


        //?????? ?????? ?????? ?????? ????????????
        //priorty==N??? ?????? ????????? ???????????? ?????? ????????? 3??? ?????? 4??? ?????? 5???
        List<Food> priorityN = foodService.findByPriority('N');
        Collections.shuffle(priorityN);

        int i=0;
        while(cnt<5) {
            list.add(priorityN.get(i));
            i++;
            cnt++;
        }


		for(Food fvo: list) {
			System.out.println(fvo.getFID());
		}


        return list;

    }


    //?????? ?????? ??????
    public List<Food> foodList(String type) {

        List<Food> foods = new ArrayList<>();

        if(type.equals("??????")) {
            foods = foodService.findAll();
        }else if(type.equals("??????")){
            List<String> exclude = new ArrayList<>();
            exclude.add("??????");
            exclude.add("??????");
            exclude.add("??????");
            exclude.add("?????????");

            foods = foodService.findByFoodCategoryNotIn(exclude);
        }else {
            foods = foodService.findByFoodCategory(type);
        }
        return foods;
    }





    //?????? ??????
    public void deleteFile(String p, String f) {

        if(f!=null) {

            File file = new File(p,f);
            file.delete();
        }

    }

    @GetMapping("/admin/foodRecommend")
    public String test(){
        return "admin/foodRecommend";
    }

    @PostMapping("/teamFoodName")
    @ResponseBody
    public String teamFoodName(@RequestParam("tID") Long tID){

        Team team = teamService.findBytID(tID);

        Food food = foodService.findByfID(team.getFood().getFID());

        String foodName = food.getFoodName();

        System.out.println(foodName);

        System.out.println(team.getTeamName());

        return foodName;
    }

    @PostMapping("/getLocationX")
    @ResponseBody
    public String getLocationX(@RequestParam("tID") Long tID){

        Team t = teamService.findBytID(tID);

        System.out.println(t.getPlaceX());

        return t.getPlaceX();
    }

    @PostMapping("/getLocationY")
    @ResponseBody
    public String getLocationY(@RequestParam("tID") Long tID){

        Team t = teamService.findBytID(tID);

        System.out.println(t.getPlaceY());

        return t.getPlaceY();
    }



}
