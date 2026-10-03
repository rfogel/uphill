package com.uphill.domain.appointment.model;

import com.uphill.domain.medic.model.Medic;
import com.uphill.domain.patient.model.Patient;
import com.uphill.domain.room.model.Room;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "appointment")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@NamedEntityGraph(
        name = "Appointment.detailed",
        attributeNodes = {
                @NamedAttributeNode(value = "medic", subgraph = "medic-subgraph"),
                @NamedAttributeNode("room"),
                @NamedAttributeNode("appointmentTime"),
                @NamedAttributeNode("patient")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "medic-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("specialty")
                        }
                )
        }
)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    private Medic medic;
    @ManyToOne
    private Room room;
    @ManyToOne
    private AppointmentTime appointmentTime;
    @ManyToOne
    private Patient patient;
}
