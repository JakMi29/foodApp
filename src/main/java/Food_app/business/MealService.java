package Food_app.business;

import Food_app.api.dto.CreateMealDTO;
import Food_app.api.dto.UpdateMealDTO;
import Food_app.api.dto.mapper.CreateMealMapper;
import Food_app.business.dao.MealDAO;
import Food_app.domain.Meal;
import Food_app.domain.Restaurant;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Service
@AllArgsConstructor
public class MealService {
    private final MealDAO mealDAO;
    private final CreateMealMapper mealMapper;
    private final RestaurantService restaurantService;
    private final OrderMealService orderMealService;
    @Autowired
    private ResourceLoader resourceLoader;

    @Transactional
    public void deleteByNameAndRestaurantName(String name, String restaurantName) {
        Restaurant restaurant = restaurantService.findRestaurantByNameWithMeals(restaurantName);
        Meal mealToDelete = mealDAO.findByNameAndRestaurant(name, restaurant);
        orderMealService.deleteAllByMeal(mealToDelete);
        mealDAO.deleteById(mealToDelete.getMealId());
    }

    @Transactional
    public Meal findByNameAndRestaurantName(String name, String restaurantName) {
        Restaurant restaurant = restaurantService.findRestaurantByNameWithMeals(restaurantName);
        return mealDAO.findByNameAndRestaurant(name, restaurant);
    }

    @Transactional
    public void addMeal(CreateMealDTO mealDto, String restaurantName) {
        Meal meal = mealMapper.map(mealDto);
        Restaurant restaurant = restaurantService.findRestaurantByNameWithMeals(restaurantName);
        Optional<Meal> existingMeal = restaurant.getMeals().stream()
                .filter(c -> c.getName().equals(meal.getName()))
                .findFirst();
        if (existingMeal.isEmpty()) {
            mealDAO.createMeal(meal
                    .withRestaurant(restaurant)
                    .withImage(createFile(mealDto.getImage())));
        } else
            throw new RuntimeException("this meal already exist in menu");
    }


    public String createFile(MultipartFile file) {
        try {
            String fileName = PhotoNumberGenerator.generatePhotoNumber(OffsetDateTime.now());
            Path uploadPath = new ClassPathResource("static/images").getFile().toPath();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return "/images/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "/images/oh_no.png";
    }

    public void deleteOldPhoto(String path) {
        try {
            Resource resource = resourceLoader.getResource("classpath:static" + path);
            File file = resource.getFile();

            if (file.exists()) {
                if (file.delete()) {
                    log.info("The file has been deleted " + path);
                } else {
                    log.info("Could not delete file: " + path);
                }
            } else {
                log.info("File does not exist: " + path);
            }
        } catch (IOException e) {
            log.info("An error occurred while deleting the file: " + path);
        }

    }

    @Transactional
    public void updateMeal(String name, String mealName, CreateMealDTO mealDto) {
        Restaurant restaurant = restaurantService.findRestaurantByNameWithMeals(name);
        Meal mealToUpdate = mealDAO.findByNameAndRestaurant(mealName, restaurant);
        deleteOldPhoto(mealToUpdate.getImage());

        mealDAO.updateMeal(
                mealToUpdate
                        .withName(mealDto.getName())
                        .withCategory(mealDto.getCategory())
                        .withPrice(new BigDecimal(mealDto.getPrice()))
                        .withDescription(mealDto.getDescription())
                        .withImage(createFile(mealDto.getImage()))
                        .withRestaurant(restaurant)
        );

    }

    @Transactional
    public void updateMealWithoutPhoto(String name, String mealName, UpdateMealDTO mealDto) {
        Restaurant restaurant = restaurantService.findRestaurantByNameWithMeals(name);
        Meal mealToUpdate = mealDAO.findByNameAndRestaurant(mealName, restaurant);

        mealDAO.updateMeal(
                mealToUpdate
                        .withName(mealDto.getName())
                        .withCategory(mealDto.getCategory())
                        .withPrice(new BigDecimal(mealDto.getPrice()))
                        .withDescription(mealDto.getDescription())
                        .withRestaurant(restaurant)
        );

    }

}