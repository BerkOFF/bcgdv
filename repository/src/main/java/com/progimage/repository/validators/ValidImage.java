package com.progimage.repository.validators;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;

import static com.progimage.repository.validators.ValidImage.SingleImageValidator.isValidImage;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.PARAMETER)
@Retention(RUNTIME)
@Constraint(validatedBy = {ValidImage.SingleImageValidator.class, ValidImage.BulkImageValidator.class})
public @interface ValidImage {
    Logger LOGGER = LoggerFactory.getLogger(ValidImage.class);
    HashSet SUPPORTED_FORMATS = Sets.newHashSet(ImageIO.getReaderFormatNames());

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SingleImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {
        @Override
        public void initialize(ValidImage constraintAnnotation) {
        }

        @Override
        public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
            return isValidImage(file, context);
        }

        static boolean isValidImage(MultipartFile file, ConstraintValidatorContext context) {
            String fileName = file.getOriginalFilename();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            try {
                if (SUPPORTED_FORMATS.contains(extension) && file.getBytes() != null && file.getBytes().length > 0) {
                    ImageIO.read(file.getInputStream());
                    return true;
                }
            } catch (IOException e) {
                LOGGER.error(String.format("Unable to read supported format [%s]", extension), e);
            }
            context.buildConstraintViolationWithTemplate(String.format("File [%s] is in unsupported image format [%s]",
                    fileName, extension)).
                    addConstraintViolation().
                    disableDefaultConstraintViolation();
            return false;
        }
    }

    class BulkImageValidator implements ConstraintValidator<ValidImage, MultipartFile[]> {
        @Override
        public void initialize(ValidImage constraintAnnotation) {
        }

        @Override
        public boolean isValid(MultipartFile[] values, ConstraintValidatorContext context) {
            return Arrays.stream(values).
                    parallel().allMatch(value -> isValidImage(value, context));
        }
    }
}
